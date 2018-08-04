// Notify native code that the page has loaded.
window.addEventListener("load", function(){ // on page load
                        // Notify native code that the page is loaded.
                        //webkit.messageHandlers.didLoad.postMessage("");
                        }, false);

var last_known_scroll_position = 0;
var ticking = false;

// Position in range [0 - 1].
var update = function(position) {
    let positionString = position.toString()
    //webkit.messageHandlers.updateProgression.postMessage(positionString);
    Android.progressionDidChange(positionString);
//    console.log("update progression position : " + positionString);
};

window.addEventListener('scroll', function(e) {
                       last_known_scroll_position = window.scrollX / document.getElementsByTagName("body")[0].scrollWidth;
                       if (!ticking) {
                       window.requestAnimationFrame(function() {
                                                    update(last_known_scroll_position);
                                                    ticking = false;
                                                    });
                       }
                       ticking = true;
                       return false;
                       });

// Scroll to the given TagId in document and snap.
var scrollToId = function(id) {
    var element = document.getElementById(id);
    var elementOffset = element.scrollLeft // element.getBoundingClientRect().left works for Gutenbergs books
    var offset = window.scrollX + elementOffset;

    document.body.scrollLeft = snapOffset(offset);
};

// Position must be in the range [0 - 1], 0-100%.
var scrollToPosition = function(position) {
    console.log("ScrollToPosition " + position);
    if ((position < 0) || (position > 1)) {
        console.log("InvalidPosition");
        return;
    }
    var offset = document.getElementsByTagName("body")[0].scrollWidth * position;

    console.log("ScrollToOffset " + offset);
    document.body.scrollLeft = snapOffset(offset);
    update(position);

};

var scrollLeft = function() {
    scrollToPosition(last_known_scroll_position)
    var offset = window.scrollX - maxScreenX;

    if (offset >= 0) {
        document.body.scrollLeft = offset;
        last_known_scroll_position = window.scrollX / document.getElementsByTagName("body")[0].scrollWidth;
        update(last_known_scroll_position);
        return "";
    } else {
        document.body.scrollLeft = 0;
        update(1.0);
        return "edge"; // Need to previousDocument.
    }
};

var scrollRight = function() {
    scrollToPosition(last_known_scroll_position)
    var offset = window.scrollX + maxScreenX;
    var scrollWidth = document.getElementsByTagName("body")[0].scrollWidth;

    if (offset < scrollWidth) {
        document.body.scrollLeft = offset;
        last_known_scroll_position = window.scrollX / document.getElementsByTagName("body")[0].scrollWidth;
        update(last_known_scroll_position);
        return "";
    } else {
        document.body.scrollLeft = scrollWidth;
        update(0.0);
        return "edge"; // Need to nextDocument.
    }
};

// Snap the offset to the screen width (page width).
var snapOffset = function(offset) {
    let value = offset + 1;

    return value - (value % maxScreenX);
};

/// User Settings.

// For setting user setting.
var setProperty = function(key, value) {
    var root = document.documentElement;

    root.style.setProperty(key, value);
};

// For removing user setting.
var removeProperty = function(key) {
    var root = document.documentElement;

    root.style.removeProperty(key);
};
