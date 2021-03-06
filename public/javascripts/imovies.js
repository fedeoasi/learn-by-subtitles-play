var dataTable;
$(document).ready(function() {
  datatable = $('#titles').dataTable( {
    //"sDom": 'T<"clear">lfrtip',
    "bProcessing": true,
    "bStateSave": true,
    "sAjaxSource": 'api/imovies',
    "sAjaxDataProp": 'imovies',
    "aaSorting": [[ 2, "desc" ]],
    "aoColumns": [
          { "mData": "otherId", "sTitle": "Other Id", "sType": "html", "sWidth": "100px" },
          { "mData": "title", "sTitle": "Title", "sClass": "movieTitle", "sWidth": "400px" },
          { "mData": "year", "sTitle": "Year", "sWidth": "200px" },
          { "mData": "genre", "sTitle": "Genre", "sWidth": "500px" },
          { "mData": "rating", "sTitle": "Rating", "sWidth": "100px" },
          { "mData": "score", "sTitle": "Score", "sWidth": "100px" },
          { "mData": "votes", "sTitle": "Votes", "sWidth": "100px" },
          { "mData": null, "sTitle": "Search", "mRender": buildLink, "sWidth": "100px", "sClass": "bg-blue" }
        ]
  });
});

function buildLink(mData, type, data) {
    return '<span><a href="/movie?search=' + data.title + '">Search</a></span>';
}
