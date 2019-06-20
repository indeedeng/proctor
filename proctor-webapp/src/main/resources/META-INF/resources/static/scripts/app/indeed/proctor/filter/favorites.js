goog.provide("indeed.proctor.filter.Favorites");

goog.require('goog.net.cookies');
goog.require('goog.pubsub.PubSub');


indeed.proctor.filter.Favorites = function (testContainer) {

    indeed.proctor.filter.Favorites.COOKIE_NAME = 'FavoriteTests';
    indeed.proctor.filter.Favorites.COOKIE_SEPARATOR = ',';

    var eventBus = new goog.pubsub.PubSub(true);
    this.model = new indeed.proctor.filter.Favorites.Model(eventBus);
    new indeed.proctor.filter.Favorites.UI(testContainer, eventBus);

    this.model.refreshFavoriteTests();
};

indeed.proctor.filter.Favorites.Model = function (eventBus) {
    this.favoriteTests = [];
    this.eventBus = eventBus;

    indeed.proctor.filter.Favorites.Model.prototype.addFavorite = function(testName) {
        this.refreshFavoriteTests();
        if (!goog.array.contains(this.favoriteTests, testName)) {
            this.favoriteTests.unshift(testName);
            this.fireUpdated();
        }
    }

    indeed.proctor.filter.Favorites.Model.prototype.removeFavorite = function(testName) {
        this.refreshFavoriteTests();
        var hasBeenremoved = goog.array.remove(this.favoriteTests, testName);
        if (hasBeenremoved) {
            this.fireUpdated();
        }
    }

    this.eventBus.subscribe('MarkedFavorite', this.addFavorite, this);
    this.eventBus.subscribe('UnMarkedFavorite', this.removeFavorite, this);
    this.eventBus.subscribe('ModelUpdated', indeed.proctor.filter.Favorites.serializeToCookie, this);
}

indeed.proctor.filter.Favorites.Model.prototype.fireUpdated = function() {
    this.eventBus.publish("ModelUpdated", this.favoriteTests);
}

indeed.proctor.filter.Favorites.Model.prototype.refreshFavoriteTests = function() {
    var testsFromCookie = indeed.proctor.filter.Favorites.deserializeFromCookie();
    if(goog.array.compare3(testsFromCookie, this.favoriteTests) !=0 ) {
        this.favoriteTests = testsFromCookie;
        this.fireUpdated();
    }
    return this.favoriteTests;
}

indeed.proctor.filter.Favorites.UI = function (testContainer, eventBus) {

    indeed.proctor.filter.Favorites.UI.FAVORITE_TOGGLE_SELECTOR = '.favorite';
    this.eventBus = eventBus;

    indeed.proctor.filter.Favorites.UI.markAsFavorite = function(favoriteToggle) {
        goog.dom.classes.add(favoriteToggle, 'favorite-toggled');
    }

    indeed.proctor.filter.Favorites.UI.removeFavorite = function(favoriteToggle) {
        goog.dom.classes.remove(favoriteToggle, 'favorite-toggled');
    }

    indeed.proctor.filter.Favorites.UI.getTestNameFromToggle = function(favoriteToggle) {
        return goog.dom.dataset.get(favoriteToggle, "testname");
    }

    indeed.proctor.filter.Favorites.UI.clickIntent = function(favoriteToggle) {
        var intentToMarkAsFavorite = !goog.dom.classes.has(favoriteToggle, 'favorite-toggled');
        return intentToMarkAsFavorite;
    }

    indeed.proctor.filter.Favorites.UI.prototype.updateView = function (favoriteTests) {
        goog.array.forEach(goog.dom.getChildren(testContainer), function (child) {
            var favoriteToggle = child.querySelector(indeed.proctor.filter.Favorites.UI.FAVORITE_TOGGLE_SELECTOR);
            var testName = indeed.proctor.filter.Favorites.UI.getTestNameFromToggle(favoriteToggle);
            if (goog.array.contains(favoriteTests, testName)) {
                indeed.proctor.filter.Favorites.UI.markAsFavorite(favoriteToggle);
            } else {
                indeed.proctor.filter.Favorites.UI.removeFavorite(favoriteToggle);
            }
        });
    }

    this.eventBus.subscribe('ModelUpdated', this.updateView, this);

    goog.array.forEach(goog.dom.getChildren(testContainer), function(child){
        var favoriteToggle = child.querySelector(indeed.proctor.filter.Favorites.UI.FAVORITE_TOGGLE_SELECTOR);
        goog.events.listen(favoriteToggle, goog.events.EventType.CLICK, function(){
            var intentToMarkAsFavorite = indeed.proctor.filter.Favorites.UI.clickIntent(favoriteToggle);
            var testName = indeed.proctor.filter.Favorites.UI.getTestNameFromToggle(favoriteToggle);
            if(intentToMarkAsFavorite) {
                eventBus.publish("MarkedFavorite", testName);
            } else {
                eventBus.publish("UnMarkedFavorite", testName);
            }
        });
    });

}

/**
 * Returns the rank of the test based on how recently the test has been marked as favorite or not.
 * Tests that have been marked as favorite most recently would have the highest rank.
 * Tests which are not marked as favorite have rank of 0
 * @param testName name of the test to check
 */
indeed.proctor.filter.Favorites.prototype.rankOf = function(testName) {
    var favoriteTests = this.model.favoriteTests;
    var index = favoriteTests.indexOf(testName);
    var numberOfFavorites = favoriteTests.length;
    if (index > -1) {
        return numberOfFavorites - index;
    } else {
        return 0;
    }
};

/**
 * Makes sure that favorites are freshly loaded from storage (cookie). This function is used to make sure
 * that the favorites are consistent between two instances of web app open in 2 browser tabs
 */
indeed.proctor.filter.Favorites.prototype.refresh = function() {
    this.model.refreshFavoriteTests();
    return this;
};


indeed.proctor.filter.Favorites.serializeToCookie = function (favoriteTests) {
    var serializedValue = favoriteTests.join(indeed.proctor.filter.Favorites.COOKIE_SEPARATOR);
    goog.net.cookies.set(indeed.proctor.filter.Favorites.COOKIE_NAME, serializedValue, 31536000, '/');
    return favoriteTests;
}

indeed.proctor.filter.Favorites.deserializeFromCookie = function () {
    var favoriteTests = [];
    var cookieValue = goog.net.cookies.get(indeed.proctor.filter.Favorites.COOKIE_NAME, '');
    if(cookieValue.length > 0) {
        favoriteTests = cookieValue.split(indeed.proctor.filter.Favorites.COOKIE_SEPARATOR);
    }
    return favoriteTests;
}
