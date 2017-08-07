/**
 * cordova Web Intent plugin
 * Copyright (c) Boris Smus 2010
 *
 */
 (function(cordova){
    var WebIntent = function() {

    };

    WebIntent.prototype.getIntent = function(success, fail) {
        return cordova.exec(function(args) {
            success(args);
        }, function(args) {
            fail(args);
        }, 'WebIntent', 'getIntent', []);
    };

    WebIntent.prototype.onNewIntent = function(callback) {
        return cordova.exec(function(args) {
            callback(args);
        }, function(args) {
        }, 'WebIntent', 'onNewIntent', []);
    };

    window.webintent = new WebIntent();
    
    // backwards compatibility
    window.plugins = window.plugins || {};
    window.plugins.webintent = window.webintent;
})(window.PhoneGap || window.Cordova || window.cordova);
