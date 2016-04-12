var result = [];

function getSubtitle() {
    $.ajax({
        url: "/api/subtitles/" + imdbId,
        context: document.body,
        success: function(data) {
            if(!data.subEntries || data.subEntries.length == 0) {
                $('#subtitlesTable').html('<span>No subtitles found</span>')
            } else {
                $('#subtitlesTable').html(buildSubsTable(data.subEntries));
            }
        },
        error: function() {
            $('#subtitlesTable').html('<span>Error retrieving subtitle</span>')
        }
    });
}

getSubtitle();