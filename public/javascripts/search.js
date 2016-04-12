var results = [];

function doSearch() {
    var title = $('#imdbSearchInput').val();
    if(title.length > 0) {
        $('#subtitles').html('');
        $('#results').html('<span><img height="50px" src="/images/ajaxSpinner.gif"/> Searching for "' + title + '"</span>')
        $.ajax({
            url: "/api/search?query=" + title,
            context: document.body,
            success: function(data) {
              results = data.results;
              $('#results').html(buildResultsTable(results));
            }
        });
    }
}

function buildResultsTable(results) {
    if(!results || results.length == 0) {
        return "<span>No results found</span>";
    }
    var div = $('<div class="cf"></div>');
    $.each(results, function(index, r) {
        var view = $('<div class="searchResult"><a><span>' + r.title + '</span></a></div>');
        view.click(function(event){ viewResult(index, event); });
        div.append(view);
    });
    return div;
}

function viewResult(index, event) {
    setResult(index);
    $('#subtitles').html(buildSubsTable(results[index].subEntries));
    $('html, body').animate({
    	scrollTop: $("#subtitles").offset().top
    }, 1000);
}

function setResult(index) {
    $.each($('#results div.searchResult'), function(i, elem) {
        if(i == index) {
            $(elem).addClass('blueB');
        } else {
            $(elem).removeClass('blueB');
        }
    });
}