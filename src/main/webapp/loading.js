(function () {
    function showLoading(message) {
        var overlay = document.getElementById("loadingOverlay");
        var messageElement = document.getElementById("loadingMessage");
        if (!overlay) {
            return;
        }
        if (messageElement && message) {
            messageElement.textContent = message;
        }
        overlay.classList.add("active");
        overlay.setAttribute("aria-hidden", "false");
    }

    document.addEventListener("DOMContentLoaded", function () {
        var triggers = document.querySelectorAll(".loading-trigger");
        for (var index = 0; index < triggers.length; index++) {
            triggers[index].addEventListener("click", function () {
                showLoading(this.getAttribute("data-loading-message") || "Processing...");
            });
        }
    });
}());
