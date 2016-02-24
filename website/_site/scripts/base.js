function getLatestReleaseInfo() {
    var release_url = 'https://api.github.com/repos/medusa-project/cantaloupe/releases/latest';
    $.getJSON(release_url).done(function (release) {
        var asset = release.assets[0];
        var downloadCount = 0;
        for (var i = 0; i < release.assets.length; i++) {
            downloadCount += release.assets[i].download_count;
        }
        var oneHour = 60 * 60 * 1000;
        var oneDay = 24 * oneHour;
        var dateDiff = new Date() - new Date(asset.updated_at);
        var timeAgo;
        if (dateDiff < oneDay) {
            timeAgo = (dateDiff / oneHour).toFixed(1) + " hours ago";
        } else {
            timeAgo = (dateDiff / oneDay).toFixed(1) + " days ago";
        }
        var releaseInfo = release.name + " was updated " + timeAgo +
            " and downloaded " + downloadCount.toLocaleString() + " times.";

        console.log('Download count: ' + downloadCount);

        $('#download-button').attr('href', asset.browser_download_url);

        // display the latest version number in the download button
        var parts = release.html_url.split('/')
        var version = parts[parts.length - 1];
        // but not betas
        if (version.indexOf('beta') == -1) {
            $('#download-button').html($('#download-button').html() + ' ' +
                version);
        }
    });
}
