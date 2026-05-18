// Exercise 7 - chat widget
(function () {
    "use strict";

    var widget = document.getElementById("chat-widget");
    if (!widget) {
        return;
    }

    var context = widget.dataset.context || "general";
    var bookId = widget.dataset.bookId || "";

    var toggle = document.getElementById("chat-toggle");
    var panel = document.getElementById("chat-panel");
    var closeBtn = document.getElementById("chat-close");
    var messages = document.getElementById("chat-messages");
    var startersBox = document.getElementById("chat-starters");
    var form = document.getElementById("chat-form");
    var input = document.getElementById("chat-input");

    var startersLoaded = false;

    function openPanel() {
        panel.classList.remove("chat-hidden");
        if (!startersLoaded) {
            loadStarters();
            startersLoaded = true;
        }
        input.focus();
    }

    function closePanel() {
        panel.classList.add("chat-hidden");
    }

    toggle.addEventListener("click", function () {
        if (panel.classList.contains("chat-hidden")) {
            openPanel();
        } else {
            closePanel();
        }
    });
    closeBtn.addEventListener("click", closePanel);

    function addMessage(text, cssClass) {
        var div = document.createElement("div");
        div.className = "chat-msg " + cssClass;
        div.textContent = text;
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
        return div;
    }

    function loadStarters() {
        var url = "/api/chat/starters?context=" + encodeURIComponent(context);
        if (bookId) {
            url += "&bookId=" + encodeURIComponent(bookId);
        }
        fetch(url)
            .then(function (r) { return r.json(); })
            .then(function (data) {
                (data.starters || []).forEach(function (starter) {
                    var btn = document.createElement("button");
                    btn.type = "button";
                    btn.className = "chat-starter";
                    btn.textContent = starter;
                    btn.addEventListener("click", function () { sendMessage(starter); });
                    startersBox.appendChild(btn);
                });
            })
            .catch(function () { });
    }

    function sendMessage(text) {
        if (!text || !text.trim()) {
            return;
        }
        startersBox.innerHTML = "";
        addMessage(text, "chat-user");
        input.value = "";
        var pending = addMessage("...", "chat-bot chat-pending");

        fetch("/api/chat", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ message: text, context: context, bookId: bookId })
        })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                pending.classList.remove("chat-pending");
                pending.textContent = data.reply || "(no response)";
            })
            .catch(function () {
                pending.classList.remove("chat-pending");
                pending.textContent = "Network error. Please try again.";
            });
    }

    form.addEventListener("submit", function (e) {
        e.preventDefault();
        sendMessage(input.value);
    });
})();
