var dataTable;
$(document).ready(function() {
  dataTable = $('#titles').dataTable( {
    //"sDom": 'T<"clear">lfrtip',
    "bProcessing": true,
    "bStateSave": true,
    "sAjaxSource": 'api/movies',
    "sAjaxDataProp": 'movies',
    "aaSorting": [[ 4, "desc" ]],
    "aoColumns": [
        { "mData": "posterUrl", "sTitle": "", "sType": "html", "sWidth": "100px" },
        { "mData": "title", "sTitle": "Title", "sClass": "movieTitle" },
        { "mData": "year", "sTitle": "Year", "sWidth": "200px" },
        { "mData": "imdbID", "sTitle": "Id", "sWidth": "200px" },
        { "mData": null, "sTitle": "Subtitles", "mRender": buildLink, "sWidth": "100px", "sClass": "bg-blue" },
        { "mData": null, "sTitle": "Action", "mRender": buildActions, "sWidth": "100px", "sClass": "bg-blue" }
    ]
  });
});

function buildLink(mData, type, data) {
    return '<span><a href="/subtitles/' + data.imdbID + '">Subtitles</a></span>';
}

function buildActions(mData, type, data) {
    return '<span><a onclick="deleteMovie(\'' + data.imdbID + '\')">Delete</a></span>';
}

function deleteMovie(id) {
    $.ajax({
        type: 'DELETE',
        url: '/api/movies/' + id,
        success: function(data, status) {
            reload();
        },
        error: function() { }
    });
}

function reload() {
    dataTable.api().ajax.reload();
}