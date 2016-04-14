var dataTable;
$(document).ready(function() {
  dataTable = $('#episodesTable').dataTable( {
    //"sDom": 'T<"clear">lfrtip',
    "bProcessing": true,
    "bStateSave": true,
    "sAjaxSource": '/api/episodes/' + imdbId,
    "sAjaxDataProp": 'episodes',
    //"aaSorting": [[ 4, "desc" ]],
    "aoColumns": [
        { "mData": "imdbID", "sTitle": "ImdbId" },
        { "mData": "season", "sTitle": "Season" },
        { "mData": "number", "sTitle": "Number" },
        { "mData": null, "sTitle": "", "mRender": buildLink, "sWidth": "100px", "sClass": "movieAction" }
    ]
  });
});

function buildLink(mData, type, data) {
    return '<span><a href="/subtitles/' + data.imdbID + '">Subtitles</a></span>';
}