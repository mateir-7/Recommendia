package com.semanticweb.bookrec.service;

import com.google.common.base.Function;
import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

// Exercise 2 - draws an rdf graph with jung
@Service
public class GraphVisualizationService {

    private static final class RdfEdge {
        private final String label;

        RdfEdge(String label) {
            this.label = label;
        }
    }

    public record RenderedGraph(byte[] image, int triples, int nodes) {
    }

    public RenderedGraph render(InputStream rdfXml) {
        Model model = ModelFactory.createDefaultModel();
        model.read(rdfXml, "", "RDF/XML");
        if (model.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file contains no RDF triples.");
        }

        Graph<String, RdfEdge> graph = new DirectedSparseMultigraph<>();
        Map<String, String> labels = new HashMap<>();
        Map<String, Boolean> isLiteralNode = new HashMap<>();
        int literalCounter = 0;
        int triples = 0;

        StmtIterator it = model.listStatements();
        while (it.hasNext()) {
            Statement st = it.nextStatement();
            triples++;

            String subjectId = nodeId(st.getSubject());
            graph.addVertex(subjectId);
            labels.put(subjectId, shortLabel(st.getSubject()));
            isLiteralNode.put(subjectId, false);

            RDFNode object = st.getObject();
            String objectId;
            if (object.isLiteral()) {
                objectId = "literal-" + (literalCounter++);
                labels.put(objectId, object.asLiteral().getLexicalForm());
                isLiteralNode.put(objectId, true);
            } else {
                objectId = nodeId(object.asResource());
                labels.put(objectId, shortLabel(object.asResource()));
                isLiteralNode.put(objectId, false);
            }
            graph.addVertex(objectId);
            graph.addEdge(new RdfEdge(localName(st.getPredicate().toString())), subjectId, objectId);
        }

        byte[] png = toPng(graph, labels, isLiteralNode);
        return new RenderedGraph(png, triples, graph.getVertexCount());
    }

    private byte[] toPng(Graph<String, RdfEdge> graph,
                         Map<String, String> labels,
                         Map<String, Boolean> isLiteralNode) {
        int side = Math.max(700, Math.min(2200, 220 + 75 * graph.getVertexCount()));
        Dimension size = new Dimension(side, side);

        // run the layout before rendering
        FRLayout<String, RdfEdge> layout = new FRLayout<>(graph, size);
        layout.initialize();
        try {
            for (int i = 0; i < 1000 && !layout.done(); i++) {
                layout.step();
            }
        } catch (RuntimeException ignored) {
        }

        VisualizationImageServer<String, RdfEdge> vv = new VisualizationImageServer<>(layout, size);
        vv.setBackground(Color.WHITE);
        vv.getRenderContext().setVertexLabelTransformer(labelFunction(labels));
        vv.getRenderContext().setEdgeLabelTransformer(edge -> edge.label);
        vv.getRenderContext().setVertexFillPaintTransformer(fillFunction(isLiteralNode));
        vv.getRenderContext().setVertexFontTransformer(node -> new Font("SansSerif", Font.BOLD, 12));
        vv.getRenderContext().setEdgeFontTransformer(edge -> new Font("SansSerif", Font.PLAIN, 11));
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);

        BufferedImage image = (BufferedImage) vv.getImage(
                new Point2D.Double(side / 2.0, side / 2.0), size);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to encode graph image", e);
        }
    }

    private Function<String, String> labelFunction(Map<String, String> labels) {
        return node -> labels.getOrDefault(node, node);
    }

    private Function<String, Paint> fillFunction(Map<String, Boolean> isLiteralNode) {
        Color resourceColor = new Color(0x4F, 0x9D, 0xD9);
        Color literalColor = new Color(0xF2, 0xC9, 0x4C);
        return node -> Boolean.TRUE.equals(isLiteralNode.get(node)) ? literalColor : resourceColor;
    }

    private String nodeId(Resource r) {
        return r.isAnon() ? "_:" + r.getId().getLabelString() : r.getURI();
    }

    private String shortLabel(Resource r) {
        return r.isAnon() ? "_:" + r.getId().getLabelString() : localName(r.getURI());
    }

    private String localName(String uri) {
        int hash = uri.lastIndexOf('#');
        int slash = uri.lastIndexOf('/');
        int cut = Math.max(hash, slash);
        return (cut >= 0 && cut < uri.length() - 1) ? uri.substring(cut + 1) : uri;
    }
}
