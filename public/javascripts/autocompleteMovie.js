$(document).ready(function() {
    $('#imdbSearchInput').autocomplete({
        serviceUrl: '/api/imovies/suggest',
        onSelect: function (suggestion) {
            var id = suggestion.data.id
            $.ajax({
                url: "/api/imovies/" + id,
                success: function(data) {
                  console.log(JSON.stringify(data));
                  renderIMovie(data);
                }
            });
        },
        onInvalidateSelection: clearInfo
    });
});

function renderIMovie(movie) {
    var posterDiv = $('<div class="posterDiv fl"><img src="' + movie.poster + '" class="big"></img></div>');
    var attrDiv = $('<div class="attrDiv fl"></div>');
    var ul = $('<ul></ul>');
    for(var p in movie) {
        if(movie.hasOwnProperty(p) && p != 'poster') {
            ul.append('<li><span class="attrKey">' + p + '</span> - <span class="attrValue">' + movie[p] + '</span></li>');
        }
    }
    ul.append('<li><button class="bg-red" onclick="addMovie(' + movie.otherId + ')">Add</button></li>');
    attrDiv.append(ul);
    var movieDiv = $('#movieDiv');
    movieDiv.html('');
    movieDiv.append(posterDiv);
    movieDiv.append(attrDiv);
}

function addMovie(movieId) {
    $.ajax({
        url: "/api/imovies/addMovie/" + movieId,
        success: function(data) {
            $('#info').html(data.info);
        },
        error: function() {
            $('#info').html('Could not add movie');
        }
    });
}

function clearInfo() {
    $('#info').html('');
}