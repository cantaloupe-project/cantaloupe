$('#download-scroller').on('click', function(e) {
    e.preventDefault();

    $('html, body').animate({
        scrollTop: $($(this).data('target')).offset().top
    }, 400);
});

/**
 * Finds the largest natural-sorted release from the GitHub API and returns it
 * in a callback.
 */
function getLatestRelease(onComplete) {
    var releasesURL = 'https://api.github.com/repos/medusa-project/cantaloupe/releases';
    $.getJSON(releasesURL).done(function(releases) {
        // Filter out betas
        releases = releases.filter(r => r.tag_name.indexOf('beta') === -1);

        // Releases are in date order. We want to display the highest version,
        // but this may not be the latest, so get a natural-sorted array of tags.
        var tags = releases.map(r => r.tag_name);
        var collator = new Intl.Collator(undefined,
            {numeric: true, sensitivity: 'base'});
        tags.sort(collator.compare);
        tags.reverse();

        // Invoke the callback with the release corresponding to the highest
        // tag version.
        var latest = releases.filter(r => r.tag_name == tags[0])[0];
        onComplete(latest);
    });
}

function injectLatestReleaseInfo() {
    getLatestRelease(function(release) {
        var button = $('.download-button');
        button.attr('href', release.assets[0].browser_download_url);
        button.html(button.html() + ' ' + release.tag_name);
    });
}
