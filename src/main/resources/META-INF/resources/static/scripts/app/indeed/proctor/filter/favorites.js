goog.provide("indeed.proctor.filter.Favorites");

goog.require('goog.net.cookies');

indeed.proctor.filter.Favorites = function (testContainer) {
    var favorites = this;
    this.testContainer = testContainer;
    this.favoriteTests = [];
    goog.array.forEach(goog.dom.getChildren(testContainer), function(child, index){
        var favoriteToggle = child.querySelector(".favorite");
        goog.events.listen(favoriteToggle, goog.events.EventType.CLICK, function(){
            favorites.toggle(favoriteToggle);
        });
    });

    //todo read cookie
};

indeed.proctor.filter.Favorites.prototype.toggle = function (favoriteToggle) {
    var testName = goog.dom.dataset.get(favoriteToggle, "testname");
    var isFavorite = this.toggleTestWithName(testName);
    if(isFavorite) {
        goog.dom.classes.add(favoriteToggle, 'favorite-toggled'); //todo move to reactive method ?
    } else {
        goog.dom.classes.remove(favoriteToggle, 'favorite-toggled');
    }
};

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

