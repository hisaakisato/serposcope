/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope */

serposcope.adminTaskController = function () {
    
    var deleteRun = function(elt) {
        $('.modal').modal('hide');
        var target = $(elt.currentTarget);
        var href = target.attr("href");
        var name = target.attr("data-name");
        var dialog = $('#modal-delete-task');
        dialog.find('form').attr('action', target.attr("href"));
        dialog.find('#delete-task-id').text(target.attr("data-id"));
        dialog.find('[name=_xsrf]').val($('#_xsrf').attr("data-value"));
        dialog.modal();
        return false;
    };

    var retryRun = function(elt) {
        $('.modal').modal('hide');
        var target = $(elt.currentTarget);
        var href = target.attr("href");
        var name = target.attr("data-name");
        var dialog = $('#modal-retry-task');
        dialog.find('form').attr('action', target.attr("href"));
        dialog.find('#retryTaskId').val(target.attr("data-id"));
        dialog.find('[name=_xsrf]').val($('#_xsrf').attr("data-value"));
        dialog.modal();
        return false;
    };

    var rescanSerp = function(elt) {
        var date = $(elt.currentTarget).attr("data-date");
        var href= $(elt.currentTarget).attr("href");
        
        if(!confirm("Confirm SERP rescan of the " + date + " run ?\n" +
            "SERP rescan is only needed when : \n" +
            "_ Website added while a task was running\n" +
            "_ Manual import of SERP in the database\n"
        )){
            return false;
        }
        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).appendTo(document.body).submit();
        
        return false;        
    };
    
    
    var render = function() {
        $('.btn-delete-run').click(deleteRun);
        $('.btn-rescan-serp').click(rescanSerp);
        $('.btn-retry-run').click(retryRun);
    };
    
    var oPublic = {
        render: render
    };
    
    return oPublic;

}();
