var dataTable;
$(document).ready(function() {
  datatable = $('#series').dataTable( {
    //"sDom": 'T<"clear">lfrtip',
    "bProcessing": true,
    "bStateSave": true,
    "sAjaxSource": 'api/series',
    "sAjaxDataProp": 'series',
    "aaSorting": [[ 4, "desc" ]],
    "aoColumns": [
          { "mData": "posterUrl", "sTitle": "", "sType": "html", "sWidth": "100px" },
          { "mData": "title", "sTitle": "Title", "sClass": "movieTitle" },
          { "mData": "year", "sTitle": "Year", "sWidth": "200px" },
          { "mData": "imdbID", "sTitle": "Id", "sWidth": "200px" },
          { "mData": null, "sTitle": "Episodes", "mRender": buildLink, "sWidth": "100px", "sClass": "movieAction" }
        ]
  });
});

function buildLink(mData, type, data) {
    return '<span><a href="/episodes/' + data.imdbID + '">Episodes</a></span>';
}