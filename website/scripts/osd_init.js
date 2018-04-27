var viewer = OpenSeadragon({
    id: "openseadragon1",
    prefixUrl: "images/openseadragon/",
    immediateRender: true,
    showNavigator: true,
    tileSources: "https://iiif.library.illinois.edu/dls/iiif/2/andromeda-pyramidal-tiled.tif"
});

$(window).on('resize', function() {
    var img_width = parseFloat($('#image-width').text());
    var img_height = parseFloat($('#image-height').text());
    var elem = $('#openseadragon1');
    if (elem.height() < 420) {
        elem.css('height', (img_height / img_width) * elem.width());
    }
}).trigger('resize');
