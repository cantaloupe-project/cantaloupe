$('#download-scroller').on('click', function(e) {
  e.preventDefault();

  $('html, body').animate({
      scrollTop: $($(this).data('target')).offset().top
  }, 400);
});

function getLatestReleaseInfo() {
    var release_url = 'https://api.github.com/repos/medusa-project/cantaloupe/releases/latest';
    $.getJSON(release_url).done(function(release) {
        var asset = release.assets[0];
        var downloadCount = 0;
        for (var i = 0; i < release.assets.length; i++) {
            downloadCount += release.assets[i].download_count;
        }

        console.debug('Download count: ' + downloadCount);

        var button = $('.download-button');
        button.attr('href', asset.browser_download_url);

        // display the latest version number in the download button
        // but not betas
        if (release.tag_name.indexOf('beta') == -1) {

            button.html(button.html() + ' ' + release.tag_name);
        }
    });
}
