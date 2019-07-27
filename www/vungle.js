var exec = require('cordova/exec');

function NyanVungle() {
    this.events = {
        onAdLoaded: function(info) { console.info(info); },
        onAdStart: function(info) { console.info(info); },
        onAdEnd: function(info) { console.info(info); },
        onAdError: function(error) { console.error(error); }
    }
}

NyanVungle.prototype.initialize = function (appId) {
    var _self = this; 

    return new Promise(function(resolve, reject) {
        function onSuccess(result) {
            if (!result.isEvent) {
                resolve(result);
            } 

            switch (result.eventName) {
                case "onAdStart": 
                    _self.events.onAdStart(result.data);
                    break;
                case "onAdEnd":
                    _self.events.onAdEnd(result.data);
                    break;
                case "onAdError":
                    _self.events.onAdError(result.data);
                    break;
                case "onAdLoaded":
                    _self.events.onAdLoaded(result.data);
                    break;
                default: 
                    return false;
                    break;
            }
            
            return true;
        }

        exec(onSuccess, reject, 'NyanVungle', 'initialize', [ appId ]);
    });
};
NyanVungle.prototype.isLoaded = function (placementId) {
    return new Promise(function(resolve, reject) {
        exec(resolve, reject, 'NyanVungle', 'isLoaded', [ placementId ]);
    });
};
NyanVungle.prototype.load = function (placementId) {
    return new Promise(function(resolve, reject) {
        exec(resolve, reject, 'NyanVungle', 'load', [ placementId ]);
    });
};
NyanVungle.prototype.show = function (placementId, options) {
    if (typeof options !== "object") {
        options = {} }

    Object.assign({
        muted: true,
        autoRotate: true
    }, options);

    return new Promise(function(resolve, reject) {
        exec(resolve, reject, 'NyanVungle', 'show', [ placementId, options.muted, options.autoRotate ]);
    });
};
NyanVungle.prototype.getPlacements = function () {
    return new Promise(function(resolve, reject) {
        exec(resolve, reject, 'NyanVungle', 'getPlacements', [ ]);
    });
};

module.exports = new NyanVungle();
