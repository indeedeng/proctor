goog.provide("indeed.proctor.filter.Favorites");

goog.require('goog.net.cookies');


indeed.proctor.filter.Favorites = function (testContainer) {

    indeed.proctor.filter.Favorites.COOKIE_NAME = 'FavoriteTests';
    indeed.proctor.filter.Favorites.COOKIE_SEPARATOR = ',';

    var favorites = this;
    this.favoriteTests = indeed.proctor.filter.Favorites.deserializeFromCookie();
    new indeed.proctor.filter.Favorites.UI(testContainer, this.favoriteTests, function(testName){return favorites.toggleTestWithName(testName)});
};

indeed.proctor.filter.Favorites.UI = function (testContainer, favoriteTests, toggleListenerFunc) {

    indeed.proctor.filter.Favorites.UI.FAVORITE_TOGGLE_SELECTOR = '.favorite';

    indeed.proctor.filter.Favorites.UI.markAsFavorite = function(favoriteToggle) {
        goog.dom.classes.add(favoriteToggle, 'favorite-toggled');
    }

    indeed.proctor.filter.Favorites.UI.removeFavorite = function(favoriteToggle) {
        goog.dom.classes.remove(favoriteToggle, 'favorite-toggled');
    }

    indeed.proctor.filter.Favorites.UI.getTestNameFromToggle = function(favoriteToggle) {
        return goog.dom.dataset.get(favoriteToggle, "testname");
    }

    goog.array.forEach(goog.dom.getChildren(testContainer), function(child){
        var favoriteToggle = child.querySelector(indeed.proctor.filter.Favorites.UI.FAVORITE_TOGGLE_SELECTOR);
        var testName = indeed.proctor.filter.Favorites.UI.getTestNameFromToggle(favoriteToggle);
        if (goog.array.contains(favoriteTests, testName)) {
            indeed.proctor.filter.Favorites.UI.markAsFavorite(favoriteToggle);
        }
    });

    goog.array.forEach(goog.dom.getChildren(testContainer), function(child){
        var favoriteToggle = child.querySelector(indeed.proctor.filter.Favorites.UI.FAVORITE_TOGGLE_SELECTOR);
        goog.events.listen(favoriteToggle, goog.events.EventType.CLICK, function(){
            var testName = indeed.proctor.filter.Favorites.UI.getTestNameFromToggle(favoriteToggle);
            var isFavorite = toggleListenerFunc(testName);
            if(isFavorite) {
                indeed.proctor.filter.Favorites.UI.markAsFavorite(favoriteToggle);
            } else {
                indeed.proctor.filter.Favorites.UI.removeFavorite(favoriteToggle);
            }
        });
    });

}

indeed.proctor.filter.Favorites.deserializeFromCookie = function () {
    var favoriteTests = [];
    var cookieValue = goog.net.cookies.get(indeed.proctor.filter.Favorites.COOKIE_NAME, '');
    if(cookieValue.length > 0) {
        favoriteTests = cookieValue.split(indeed.proctor.filter.Favorites.COOKIE_SEPARATOR);
    }
    return favoriteTests;
}

indeed.proctor.filter.Favorites.prototype.serializeToCookie = function () {
    var favoriteTests = this.favoriteTests;
    var serializedValue = favoriteTests.join(indeed.proctor.filter.Favorites.COOKIE_SEPARATOR);
    goog.net.cookies.set(indeed.proctor.filter.Favorites.COOKIE_NAME, serializedValue, 31536000, '/');
    return favoriteTests;
}

indeed.proctor.filter.Favorites.prototype.toggleTestWithName = function(testName) {
    var index = this.favoriteTests.indexOf(testName);
    var hasBeenMarkedAsFavorite;
    if (index > -1) {
        this.favoriteTests.splice(index, 1);
        hasBeenMarkedAsFavorite = false;
    } else {
        this.favoriteTests.unshift(testName);
        hasBeenMarkedAsFavorite = true;
    }
    this.serializeToCookie();
    return hasBeenMarkedAsFavorite;
};

/**
 * Returns the rank of the test based on how recently the test has been marked as favorite or not.
 * Tests that have been marked as favorite most recently would have the highest rank.
 * Tests which are not marked as favorite have rank of 0
 * @param testName name of the test to check
 */
indeed.proctor.filter.Favorites.prototype.rankOf = function(testName) {
    var index = this.favoriteTests.indexOf(testName);
    var numberOfFavorites = this.favoriteTests.length;
    if (index > -1) {
        return numberOfFavorites - index;
    } else {
        return 0;
    }
};

