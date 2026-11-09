(function () {
    "use strict";

    /* 导航栏滚动状态 */
    var nav = document.getElementById("nav");
    function onScroll() {
        if (window.scrollY > 8) {
            nav.classList.add("is-scrolled");
        } else {
            nav.classList.remove("is-scrolled");
        }
    }
    window.addEventListener("scroll", onScroll, {passive: true});
    onScroll();

    /* 移动端菜单 */
    var toggle = document.getElementById("navToggle");
    var links = document.getElementById("navLinks");
    if (toggle && links) {
        toggle.addEventListener("click", function () {
            links.classList.toggle("is-open");
        });
        links.addEventListener("click", function (e) {
            if (e.target.tagName === "A") {
                links.classList.remove("is-open");
            }
        });
    }

    /* 滚动进入动画 */
    var revealEls = document.querySelectorAll(".reveal");
    if ("IntersectionObserver" in window) {
        var io = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add("is-visible");
                    io.unobserve(entry.target);
                }
            });
        }, {threshold: 0.12, rootMargin: "0px 0px -40px 0px"});
        revealEls.forEach(function (el) {
            io.observe(el);
        });
    } else {
        revealEls.forEach(function (el) {
            el.classList.add("is-visible");
        });
    }

    /* 复制构建命令 */
    var copyBtn = document.getElementById("copyBtn");
    var buildCmd = document.getElementById("buildCmd");
    if (copyBtn && buildCmd) {
        copyBtn.addEventListener("click", function () {
            var text = buildCmd.textContent;
            var done = function () {
                copyBtn.textContent = "已复制 ✓";
                setTimeout(function () {
                    copyBtn.textContent = "复制";
                }, 1600);
            };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text).then(done, done);
            } else {
                var ta = document.createElement("textarea");
                ta.value = text;
                document.body.appendChild(ta);
                ta.select();
                try {
                    document.execCommand("copy");
                } catch (e) {
                }
                document.body.removeChild(ta);
                done();
            }
        });
    }

    /* 年份 */
    var year = document.getElementById("year");
    if (year) {
        year.textContent = new Date().getFullYear();
    }
})();
