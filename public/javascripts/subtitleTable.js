function buildSubsTable(data) {
    var table = $('<table>' +
        '<thead><tr>' +
        '<th>Number</th>' +
        '<th>Start</th>' +
        '<th>Stop</th>' +
        '<th></th>' +
        '</tr></thead></table>').addClass('standardTable');
    var tbody = $('<tbody></tbody>');
    $.each(data, function(index, r) {
    var row = $('<tr>' +
        '<td>' + r.number + '</td>' +
        '<td>' + r.start + '</td>' +
        '<td>' + r.stop + '</td>' +
        '<td>' + r.text + '</td>' +
        '</tr>')
    tbody.append(row);
    });
    table.append(tbody);
    return table;
}