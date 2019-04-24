/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope, canonLoc, Papa */

serposcope.googleGroupController = function () {
    
    var onResize = function(){
        $('.tab-content').css("min-height", serposcope.theme.availableHeight() - 150);
        serposcope.googleGroupControllerGrid.resize();
    };
    
    var configureModalFocus = function() {
        $('#new-target').on('shown.bs.modal', function(){ $('#targetName').focus(); });
        $('#new-target-bulk').on('shown.bs.modal', function(){ $('#bulk-target').focus(); });
        $('#new-search').on('shown.bs.modal', function(){ $('#searchName').focus(); });
        $('#new-search-bulk').on('shown.bs.modal', function(){ $('#bulk-search').focus(); });
    };
    
    var showNewSearchModal = function(){
        $('.modal').modal('hide');
        $('#new-search').modal();
        return false;
    };
    
    var showNewBulkSearchModal = function(){
        $('.modal').modal('hide');
        $('#new-search-bulk').modal();
        return false;
    };
    
    var showNewTargetModal = function(){
        $('.modal').modal('hide');
        $('#new-target').modal();
        return false;
    };
    
    var showNewBulkTargetModal = function(){
        $('.modal').modal('hide');
        $('#new-target-bulk').modal();
        return false;
    };    
    
    var showNewEventModal = function(elt){
        $('#modal-add-event').modal();
        return false;
    };    
    
    var showExportSerpsModal = function(elt){
        var form = $('#modal-export-serps').find('form');
        form.find('[name=searchIds]').remove();
        var ids = serposcope.googleGroupControllerGrid.getSelection();
        for(var i=0; i <ids.length; i++){
           form.append($('<input>', {
                'name': 'searchIds',
                'value': ids[i],
                'type': 'hidden'
            }));
        }
        $('#modal-export-serps').modal();
        return false;
    };    
    
    var deleteTarget = function(elt){
        var id = $(elt.currentTarget).attr("data-id");
        var name = $("#target-" + id +" .target-name").html();
        var href= $(elt.currentTarget).attr("href");
        
        if(!confirm("Delete website \"" + name + "\" ?\nAll history will be erased.")){
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
        })).append($('<input>', {
            'name': 'id[]',
            'value': id,
            'type': 'hidden'
        })).appendTo(document.body).submit();
        
        return false;
    };
    
    
    var deleteTargets = function(elt){
        if(!confirm("Delete targets ?\nAll history will be erased.")){
            return false;
        }        
        
        $('<form>', {
            'action': $(elt.currentTarget).attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).append($('.chk-target'))
        .appendTo(document.body).submit();        
        
        return false;
    };    
    
    var deleteSearches = function(elt){
        if(!confirm("Delete searches ?\nAll history will be erased.")){
            return false;
        }
        
        var form = '<form method="post" target="_top" action="' + $(elt.currentTarget).attr("data-action") + '">';
        form += '<input type="hidden" name="_xsrf" value="' + $('#_xsrf').attr("data-value") + '">';
        
        var ids = serposcope.googleGroupControllerGrid.getSelection();
        for(var i=0; i <ids.length; i++){
        	form += '<input type="hidden" name="id[]" value="' + ids[i] + '">';
        }
        form += '</form>';

        $(form).appendTo(document.body).submit();
        return false;
    };
    
    var bulkTargetSubmit = function() {
        var patterns = [];
        if($('#bulk-target').val() == ""){
            alert("no target specified");
            return false;
        }
        
        var lines = $('#bulk-target').val().split(/\r?\n/);
        for(var i = 0; i< lines.length; i++){
            lines[i] = lines[i].replace(/(^\s+)|(\s+$)/g,"");
            if(lines[i].length == 0){
                continue;
            }
            
            patterns.push(lines[i]);
        }
        
        if(patterns.length == 0){
            alert("no target specified");
            return false;
        }        
    
        var form = $('<form>', {
            'action': $("#bulk-target-import").attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).append($('<input>', {
            'name': 'target-radio',
            'value': $('#new-target-bulk .target-radio:checked').val(),
            'type': 'hidden'
        }));
        
        var inputs = [];
        for(var i = 0; i< patterns.length; i++){
            inputs.push($('<input>', {'name': 'name[]','value': patterns[i],'type': 'hidden'})[0]);
            inputs.push($('<input>', {'name': 'pattern[]','value': patterns[i],'type': 'hidden'})[0]);
        }        
        form.append(inputs);
        form.appendTo(document.body).submit();
        return false;
    };
    
    var bulkSearchSubmit = function(){
        if($('#bulk-search').val() == ""){
            alert("no search specified");
            return false;
        }

        var keyword = [], country = [], device = [], local = [], custom = [];
        var defaultDevice = parseInt($('#csp-vars').attr('data-default-device'));
        var defaultCountry = $('#csp-vars').attr('data-default-country');
        var defaultLocal = $('#csp-vars').attr('data-default-local');
        var defaultCustom = $('#csp-vars').attr('data-default-custom');
        
        var lines = $('#bulk-search').val().split(/\r?\n/);
        
        for(var i = 0; i< lines.length; i++){
            lines[i] = lines[i].replace(/(^\s+)|(\s+$)/g,"");
            if(lines[i].length == 0){
                continue;
            }
            
            var params = Papa.parse(lines[i]);
            if(params.data.length != 1){
                alert("error at line " + i + " : " + lines[i]);
                return;
            }
            params = params.data[0];
            if(typeof(params[0]) == "undefined"){
                continue;
            }
            keyword.push(params[0]);
            if(params.length > 1){
                switch(params[1].toLowerCase()){
                    case "desktop":
                    case "pc":
                        device.push(0);
                        break;
                    case "smartphone":
                    case "mobile":
                    case "sp":
                        device.push(1);
                        break;  
                    default:
                        alert(params[1] + " is an invalid device type, valid values : desktop, pc, mobile, sp");
                        return false;
                }
            } else {
                device.push(defaultDevice);
            }
            country.push(params.length <= 2 || defaultCountry === params[2] ? '' : params[2]);
            local.push(params.length <= 3 || defaultLocal === params[3] ? '' : params[3]);
            custom.push(params.length <= 4 || defaultCustom === params[4] ? '' : params[4]);
        }
        
        var form = '<form method="post" target="_top" action="' + $("#bulk-search-import").attr("data-action") + '">';
        form += '<input type="hidden" name="_xsrf" value="' + $('#_xsrf').attr("data-value") + '">';
        for(var i=0; i<keyword.length; i++){
        	form += '<input type="hidden" name="keyword[]" value="' + keyword[i] + '">'
            		+ '<input type="hidden" name="country[]" value="' + country[i] + '">'
            		+ '<input type="hidden" name="device[]" value="' + device[i] + '">'
            		+ '<input type="hidden" name="local[]" value="' + local[i] + '">'
            		+ '<input type="hidden" name="custom[]" value="' + custom[i] + '">';
        }
        form += '</form>'
        $(form).appendTo(document.body).submit();
        return false;
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
    
    var toggleEvent = function(elt) {
        $('#event-description-' + $(elt.currentTarget).attr('data-id')).toggleClass("hidden");
    };
    
    var deleteEvent = function(elt){
        var day = $(elt.currentTarget).attr("data-day");
        var href= $(elt.currentTarget).attr("href");
        
        if(!confirm("Delete event \"" + day + "\" ?")){
            return false;
        }
        
        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': 'day',
            'value': day,
            'type': 'hidden'
        })).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).appendTo(document.body).submit();
        
        return false;        
    };
    
    var renameGroup = function(elt){
        var target = $(elt.currentTarget);
        var form = $("#rename-group-form");
        form.attr("action", target.attr("href"));
        form.find("#groupName").val(target.attr("data-name"));
        form.find("#groupShared").prop("checked", target.attr("data-shared") === "true");
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
    
    var renameTarget = function(elt){
        var href = $(elt.currentTarget).attr("href");
        var id = $(elt.currentTarget).attr("data-id");
        var name = prompt("new name");
        
        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': 'name',
            'value': name,
            'type': 'hidden'
        })).append($('<input>', {
            'name': 'id',
            'value': id,
            'type': 'hidden'
        })).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).appendTo(document.body).submit();
        
        return false;
    };    
    
    var onRadioTargetChange = function(){
        $("#pattern").attr('placeholder', $(this).attr("data-help"));
    };
    
    var searchChecked = false;
    var checksearch = function(){
        $('.chk-search').prop('checked',searchChecked=!searchChecked ? 'checked' : '');
        return false;
    };
    
    var targetChecked = false;
    var checkTarget = function(){
        $('.chk-target').prop('checked',targetChecked=!targetChecked ? 'checked' : '');
        return false;
    };    
    
    var exportSearches = function(elt){
        var $form = $('<form>', {
            'action': $(elt.currentTarget).attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        }));
        
        var ids = serposcope.googleGroupControllerGrid.getSelection();
        for(var i=0; i <ids.length; i++){
           $form.append($('<input>', {
                'name': 'id[]',
                'value': ids[i],
                'type': 'hidden'
            }));
        }
        $form.appendTo(document.body).submit();
        return false;
    };

    var exportTargets = function(elt){
        var $form = $('<form>', {
            'action': $(elt.currentTarget).attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        }));
        
        $('.chk-target').each(function() {
        	var checkbox = $(this);
            if (checkbox.prop('checked')) {
                $form.append($('<input>', {
                    'name': 'id[]',
                    'value': checkbox.val(),
                    'type': 'hidden'
                }));
            }
        });
        $form.appendTo(document.body).submit();
        return false;
    };

    var loadAsyncCanonical = function() {
        $.ajax({
            url: '/assets/js/canonical-location.js',
            dataType: 'script',
            cache: true, // otherwise will get fresh copy every page load
            success: function () {
                configureSearchLocal();
            }
        });
    };
    
    var configureSearchLocal = function(){
        $('.search-local').typeahead({
            source: canonLoc,
            minLength: 2,
            items: 100,
            matcher: function(arg){
                var item = arg;
                var array = this.query.split(" ");
                for(var i=0; i<array.length; i++){
                    if( item.indexOf(array[i]) === -1){
                        return false;
                    }
                }
                return true;
            },
            highlighter: function (item) {return item;}
        });
    };
    
    var renderScoreHistory = function() {
        $('.score-history-inline').sparkline("html", {tagValuesAttribute: "data-values"});        
    };
    
    var configureTabs = function() {
        $('.nav-tabs a').on('shown.bs.tab', function (e) {
            window.location.hash = e.target.hash;
            window.scrollTo(0, 0);
            if(e.target.hash == "#tab-searches"){
                serposcope.googleGroupControllerGrid.resize();
            }
        });
        
        var url = document.location.toString();
        if (url.match('#')) {
            $('.nav-tabs a[href="#' + url.split('#')[1] + '"]').tab('show');
        } 
    };
    
    var showRunGroupModal = function () {
        $('#run-group-modal').modal();
        return false;
    };
    
    var view = function() {
        $(window).bind("load resize", onResize);
        $('input[name="day"]').daterangepicker({
            singleDatePicker: true,
            locale: {
                format: 'YYYY-MM-DD'
            }
        });
        configureModalFocus();
        $('.target-radio').change(onRadioTargetChange);
        $("#pattern").attr('placeholder', $('#target-domain').attr("data-help"));
        $('.btn-rename').click(renameGroup);
        $('.btn-rename-target').click(renameTarget);
        $('.toggle-event').click(toggleEvent);
        $('.btn-add-event').click(showNewEventModal);
        $('.btn-delete-event').click(deleteEvent);
        $('.btn-run-group').click(showRunGroupModal);
        
        $('.btn-delete-group').click(deleteGroup);
        $('.btn-rename-group').click(renameGroup);
        $('.btn-add-target').click(showNewTargetModal);
        $('.btn-add-target-bulk').click(showNewBulkTargetModal);
        $('.btn-add-search').click(showNewSearchModal);
        $('.btn-add-search-bulk').click(showNewBulkSearchModal);
        $('#bulk-search-import').click(bulkSearchSubmit);
        $('#bulk-target-import').click(bulkTargetSubmit);
        
        $('.btn-delete-target').click(deleteTarget);
        
        $('#btn-chk-search').click(checksearch);
        $('#btn-chk-target').click(checkTarget);
        $('#btn-export-searches').click(exportSearches);
        $('#modal-export-serps input[name=targetOnly]').change(function(e) {
        	if (e.target.checked) {
            	$('#modal-export-serps input[name=firstTargetOnly]').removeAttr('disabled').parent().show();
        	} else {
            	$('#modal-export-serps input[name=firstTargetOnly]').attr('disabled', 'disabled').parent().hide();
        	}
        });
        $('#btn-export-serps').click(showExportSerpsModal);
        $('#btn-export-targets').click(exportTargets);
        $('#btn-delete-searches').click(deleteSearches);
        $('#btn-delete-targets').click(deleteTargets);
        $('#table-target').stupidtable();

        //$('.btn-start-task').click(showNewTargetModal);

        var today = new Date().format('yyyy-mm-dd');
        $('#daterange-serps').removeAttr("disabled").attr('readonly', true).css('background-color', '#ffffff');
        $('#daterange-serps').daterangepicker({
            "ranges": {
                'Last day': [moment(today), moment(today)],
                'Last 30 days': [moment(today).subtract(30, 'days'), moment(today)],
                'Current Month': [moment(today).startOf('month'), moment(today).endOf('month')],
                'Previous Month': [moment(today).subtract(1, 'month').startOf('month'), moment(today).subtract(1, 'month').endOf('month')]
            },
            locale: {
              format: 'YYYY-MM-DD'
            },
            showDropdowns: true,
            startDate: today,
            endDate: today
        }, function(startDate, endDate) {
        	$('#daterange-serps-startdate').val(startDate.format('YYYY-MM-DD'))
        	$('#daterange-serps-enddate').val(endDate.format('YYYY-MM-DD'))
        });

        renderScoreHistory();
        configureTabs();
        serposcope.googleGroupControllerGrid.render();
        loadAsyncCanonical();
    };
    
    var oPublic = {
        view: view
    };
    
    return oPublic;

}();
