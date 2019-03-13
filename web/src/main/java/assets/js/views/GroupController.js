/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope */

serposcope.GroupController = function () {
    
    
    var resizeTabContent = function(){
        $('.tab-content.tab-groups').css("min-height", serposcope.theme.availableHeight() - 100);
    };
    
    var deleteGroup = function(elt) {
        $('.modal').modal('hide');
        var target = $(elt.currentTarget);
        var href = target.attr("href");
        var name = target.attr("data-name");
        var dialog = $('#modal-delete-group');
        dialog.find('form').attr('action', target.attr("href"));
        dialog.find('#delete-group-name').text(target.attr("data-name"));
        dialog.find('[name=_xsrf]').val($('#_xsrf').attr("data-value"));
        dialog.modal();
        return false;
    };
    
    var renameGroup = function(elt){
        var target = $(elt.currentTarget);
        var form = $("#rename-group-form")
        form.attr("action", target.attr("href"));
        form.find("#groupName").val(target.attr("data-name"));
        form.find("#groupSundayEnabled").prop("checked", target.attr("data-sunday-enabled") === "true");
        form.find("#groupMondayEnabled").prop("checked", target.attr("data-monday-enabled") === "true");
        form.find("#groupTuesdayEnabled").prop("checked", target.attr("data-tuesday-enabled") === "true");
        form.find("#groupWednesdayEnabled").prop("checked", target.attr("data-wednesday-enabled") === "true");
        form.find("#groupThursdayEnabled").prop("checked", target.attr("data-thursday-enabled") === "true");
        form.find("#groupFridayEnabled").prop("checked", target.attr("data-friday-enabled") === "true");
        form.find("#groupSaturdayEnabled").prop("checked", target.attr("data-saturday-enabled") === "true");
        $('.modal').modal('hide');
        $('#rename-group-modal').modal();
        return false;
    };
    
    var render = function() {
        $(window).bind("load resize", function () {
            resizeTabContent();
        });
        $('.btn-delete-group').click(deleteGroup);
        $('.btn-rename-group').click(renameGroup);
        $('#sidebar-group-search').typeahead({
            source: serposcope.sidebar.groupSuggest,
            minLength: 0,
            showHintOnFocus: true,
            highlighter: serposcope.sidebar.groupHighlighted,
            afterSelect: serposcope.sidebar.groupSelected
        });
    };
    
    var oPublic = {
        render: render
    };
    
    return oPublic;

}();
