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
        releases = releases.filter(e => e.tag_name.indexOf('beta') === -1);

        // Natural sort
        releases.sort(function(a, b) {
            return +/\d+/.exec(a.tag_name)[0] < +/\d+/.exec(b.tag_name)[0];
        });

        onComplete(releases[0]);
    });
}

function injectLatestReleaseInfo() {
    getLatestRelease(function(release) {
        var button = $('.download-button');
        button.attr('href', release.assets[0].browser_download_url);
        button.html(button.html() + ' ' + release.tag_name);
    });
}
