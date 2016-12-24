$(function () {
    var dataTable,
        defaultQueryUrl = "api/v1/parties?q_sort=+identity",
        select_all = $("#select_all"),
        select_all_page_checked = [];

    function initPartyForm() {
        var partyFormDialog, partyForm = $("#party_form"), parentsTable, childrenTable,
            // From http://www.whatwg.org/specs/web-apps/current-work/multipage/states-of-the-type-attribute.html#e-mail-state-%28type=email%29
            emailRegex = /^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/,
            identity = $( "#identity" ),
            name = $( "#name" ),
            email = $( "#email" ),
            enabled = $("#enabled"),
            allFields = $( [] ).add(identity).add( name ).add( email ).add(enabled),
            tips = $( ".validateTips" ),
            parentsIdentity = $("#parents_identity"),
            childrenIdentity = $("#children_identity");

        partyFormDialog = $( "#party_form_dialog" ).dialog({
            autoOpen: false,
            modal: true,
            height: 800,
            width: 600,
            buttons: {
                "Confirm": createOrUpdateParty,
                Cancel: function() {
                    partyFormDialog.dialog( "close" );
                }
            },
            close: function() {
                partyForm[0].reset();
                allFields.removeClass( "ui-state-error" );
                parentsIdentity.val("");
                childrenIdentity.val("");
            }
        });

        partyForm.on( "submit", function( event ) {
            createOrUpdateParty();
            event.preventDefault();
        });

        parentsTable = $("#parents_table").DataTable({
            searching: false,
            paging: false,
            info: false,
            data: [],
            columns: [
                {
                    data: "id",
                    visible: false
                },
                {data: "identity"},
                {data: "name"},
                {data: "type"},
                {
                    searchable: false,
                    orderable: false,
                    width:"1%",
                    "render": function (){
                        return "<input type=\"button\" name=\"remove\" value=\"Remove\"/>";
                    }
                }
            ],
            order: [[1, "asc"]]
        });

        parentsTable.on( "click", "input[type=button]", function() {
            parentsTable.row($(this).parents("tr")).remove().draw(false);
        });

        childrenTable = $("#children_table").DataTable({
            searching: false,
            paging: false,
            info: false,
            data: [],
            columns: [
                {
                    data: "id",
                    visible: false
                },
                { data: "identity"},
                { data: "name" },
                { data: "type" },
                {
                    searchable: false,
                    orderable: false,
                    width:"1%",
                    "render": function (){
                        return "<input type=\"button\" name=\"remove\" value=\"Remove\"/>";
                    }
                }
            ],
            order: [[ 1, "asc" ]]
        });

        childrenTable.on( "click", "input[type=button]", function() {
            childrenTable.row($(this).parents("tr")).remove().draw(false);
        });

        $( "#create_user" ).on( "click", function() {
            partyFormDialog.dialog("option", "partyType", "user");
            partyFormDialog.dialog("option", "action", "create");
            partyFormDialog.dialog("option", "title", "Create User");
            initPartyRelationTable("user", [], []);
            partyFormDialog.dialog("open");
        });
        $( "#create_org" ).on( "click", function() {
            partyFormDialog.dialog("option", "partyType", "organization");
            partyFormDialog.dialog("option", "action", "create");
            partyFormDialog.dialog("option", "title", "Create Organization");
            initPartyRelationTable("organization", [], []);
            partyFormDialog.dialog("open");
        });
        $( "#create_group" ).on( "click", function() {
            partyFormDialog.dialog("option", "partyType", "group");
            partyFormDialog.dialog("option", "action", "create");
            partyFormDialog.dialog("option", "title", "Create Group");
            initPartyRelationTable("group", [], []);
            partyFormDialog.dialog("open");
        });

        $( "#party_table tbody" ).on( "click", "tr", function(event) {
            if($(event.target).hasClass("select-checkbox")) {
                return;
            }

            $.ajax({
                url:"api/v1/parties/" + dataTable.row( this ).data().id + "?q_fetchRelations=parents,children",
                type:"GET",
                dataType:"json",
                success: function(response) {
                    var party = response;
                    partyFormDialog.dialog("option", "party", party);
                    partyFormDialog.dialog("option", "partyType", party.type);
                    partyFormDialog.dialog("option", "action", "update");
                    partyFormDialog.dialog("option", "title", "Update " + party.type.charAt(0).toUpperCase() + party.type.slice(1));
                    identity.val(party.identity);
                    name.val(party.name);
                    email.val(party.email);
                    enabled.prop("checked", party.enabled === true);
                    initPartyRelationTable(party.type, party.parents, party.children);
                    partyFormDialog.dialog("open");
                },
                error: function(xhr) {
                    alert("fetch party failure: " + xhr.responseText);
                }
            });
        });

        function initPartyRelationTable(partyType, parents, children) {
            parentsTable.clear();
            childrenTable.clear();
            parentsTable.rows.add(parents).draw(false);
            childrenTable.rows.add(children).draw(false);

            parentsIdentity.autocomplete(buildAutoCompleteOption(true));
            childrenIdentity.autocomplete(buildAutoCompleteOption(false));
            if(partyType === "user") {
                $("#children_auto_complete").hide();
            }
            else {
                $("#children_auto_complete").show();
            }

            function buildAutoCompleteOption(isParent) {
                return {
                    source: function(request, autoCompleteResponse){
                        $.ajax({
                            url:"api/v1/parties?q_predicates=[identity like " + request.term + "%25 ; "
                            + (isParent ? buildParentsTypeQueryPredicate(partyType) : buildChildrenTypeQueryPredicate(partyType)) + "]",
                            type: "GET",
                            dataType: "json",
                            success: function(response) {
                                var autoCompleteData = response.map(function(party) {
                                    return {
                                        value: party.identity + "<" + party.name + ">@" + party.type,
                                        party: party
                                    };
                                });
                                autoCompleteResponse(autoCompleteData);
                            },
                            error: function(xhr) {
                                autoCompleteResponse([]);
                                alert("fetch party failure: " + xhr.responseText);
                            }
                        });
                    },
                    minLength: 2,
                    select: function(event, ui) {
                        var rowData = {
                            id: ui.item.party.id,
                            identity: ui.item.party.identity,
                            name: ui.item.party.name,
                            type: ui.item.party.type
                        };
                        if (isParent) {
                            parentsTable.row.add(rowData).draw(false);
                        }
                        else {
                            childrenTable.row.add(rowData).draw(false);
                        }
                        $(this).val("");
                        return false;
                    }
                };
            }

            function buildChildrenTypeQueryPredicate(partyType) {
                if(partyType === "group") {
                    return "TYPE(party) in (User,Organization,Group)";
                }
                else if(partyType === "organization") {
                    return "TYPE(party) in (User,Organization)";
                }
                else {
                    throw new Error("invalid party type " + partyType);
                }
            }

            function buildParentsTypeQueryPredicate(partyType) {
                if (partyType === "user") {
                    return "TYPE(party) in (Organization,Group)";
                }
                else if(partyType === "group") {
                    return "TYPE(party) = \'Group\'";
                }
                else if(partyType === "organization") {
                    return "TYPE(party) in (Organization,Group)";
                }
                else {
                    throw new Error("invalid party type " + partyType);
                }
            }
        }

        function updateTips( t ) {
            tips.text( t ).addClass( "ui-state-highlight" );
            setTimeout(function() {
                tips.removeClass( "ui-state-highlight", 1500 );
            }, 500 );
        }

        function checkLength( o, n, min, max ) {
            if ( o.val().length > max || o.val().length < min ) {
                o.addClass( "ui-state-error" );
                updateTips( "Length of " + n + " must be between " + min + " and " + max + "." );
                return false;
            } else {
                return true;
            }
        }

        function checkRegexp( o, regexp, n ) {
            if ( !( regexp.test( o.val() ) ) ) {
                o.addClass( "ui-state-error" );
                updateTips( n );
                return false;
            } else {
                return true;
            }
        }

        function validateParty() {
            var valid = true;
            allFields.removeClass( "ui-state-error" );

            valid = valid && checkLength( identity, "identity", 2, 30);
            valid = valid && checkLength( name, "name", 1, 30 );
            valid = valid && checkRegexp( identity, /^[a-zA-z]([0-9A-Za-z_-])+$/i, "identity may consist of a-z, A-Z, 0-9, underscores, spaces and must begin with a letter." );
            if(email.val() !== "") {
                valid = valid && checkLength( email, "email", 1, 80 );
                valid = valid && checkRegexp( email, emailRegex, "eg. abc@def.com" );
            }
            return valid;
        }

        function createOrUpdateParty() {
            var valid = validateParty();
            if (valid) {
                var action = partyFormDialog.dialog("option", "action");
                var partyType = partyFormDialog.dialog("option", "partyType");
                var party = {
                    identity: identity.val(),
                    type: partyType,
                    name: name.val(),
                    email: email.val(),
                    enabled: enabled.is(":checked"),
                    parents: parentsTable.data().toArray(),
                    children: childrenTable.data().toArray()
                };

                if(action === "create") {
                    $.ajax({
                        url:"api/v1/" + partyType + "s",
                        type:"POST",
                        data:JSON.stringify(party),
                        dataType:"json",
                        contentType:"application/json",
                        success: function() {
                            partyFormDialog.dialog( "close" );
                            reloadData();
                        },
                        error: function(xhr) {
                            alert("create party failure: " + xhr.responseText);
                        }
                    });
                }
                else {
                    var oldParty = partyFormDialog.dialog("option", "party");
                    party.id = oldParty.id;
                    party.version = oldParty.version;
                    $.ajax({
                        url:"api/v1/" + partyType + "s/" + party.id,
                        type:"PUT",
                        data:JSON.stringify(party),
                        contentType:"application/json",
                        success: function() {
                            partyFormDialog.dialog( "close" );
                            reloadData();
                        },
                        error: function(xhr) {
                            alert("update party failure: " + xhr.responseText);
                        }
                    });
                }
            }
            return valid;
        }
    }
    initPartyForm();

    function initDataTable() {
        dataTable = $("#party_table").DataTable({
            searching: false,
            ajax: {
                url: defaultQueryUrl,
                type: "GET",
                dataSrc: "",
                beforeSend: function (request) {
                    request.setRequestHeader("Accept", "application/json");
                }
            },
            columns: [
                {
                    searchable: false,
                    orderable: false,
                    className: "select-checkbox",
                    width:"1%",
                    "render": function (){
                        return "";
                    }
                },
                {
                    data: "id",
                    visible: false
                },
                { data: "identity"},
                { data: "name" },
                { data: "type" },
                { data: "enabled" }
            ],
            select: {
                style:    "multi",
                selector: "td:first-child"
            },
            order: [[ 1, "asc" ]]
        });

        function initSelectAll() {
            select_all.click(function(){
                if(this.checked) {
                    dataTable.rows({page: "current"}).select();
                    select_all_page_checked[dataTable.page()] = true;
                }
                else {
                    dataTable.rows({page: "current"}).deselect();
                    select_all_page_checked[dataTable.page()] = false;
                }
            });

            dataTable.on( "page.dt", function () {
                var page = dataTable.page();
                select_all.prop("checked", select_all_page_checked[page] === undefined ? false : select_all_page_checked[page]);
            });
        }
        initSelectAll();
    }
    initDataTable();

    function initSelectAction() {
        $("#delete").click(function() {
            $.ajax({
                url:"api/v1/parties",
                type:"DELETE",
                data: JSON.stringify(getSelectedRowsId()),
                contentType:"application/json",
                success: function() {
                    reloadData();
                },
                error: function(xhr) {
                    alert("delete party failure: " + xhr.responseText);
                }
            });
        });
        $("#enable").click(function() {
            $.ajax({
                url:"api/v1/parties/enable",
                type:"PUT",
                data: JSON.stringify(getSelectedRowsId()),
                contentType:"application/json",
                success: function() {
                    reloadData();
                },
                error: function(xhr) {
                    alert("enable party failure: " + xhr.responseText);
                }
            });
        });
        $("#disable").click(function() {
            $.ajax({
                url:"api/v1/parties/disable",
                type:"PUT",
                data: JSON.stringify(getSelectedRowsId()),
                contentType:"application/json",
                success: function() {
                    reloadData();
                },
                error: function(xhr) {
                    alert("disable party failure: " + xhr.responseText);
                }
            });
        });

        function getSelectedRowsId() {
            var ids = dataTable.rows(".selected").data().map(function(data) {
                return "\"" + data.id + "\"";
            }).join(",");
            return JSON.parse("[" + ids + "]");
        }
    }
    initSelectAction();

    function initSearchForm() {
        $("#search_form").on("submit", function(e) {
            var predicate = buildPredicateQueryString();
            var url = defaultQueryUrl + (predicate === "" ? "" : "&" + predicate);
            reloadData(url);
            e.preventDefault();
        });

        function buildPredicateQueryString() {
            var identity = $("input[name=search_identity]").val();
            var name = $("input[name=search_name]").val();
            var types = $("input[name=search_type]:checked").map(function() {
                return "'" + this.value + "'";
            }).get().join();
            var enabled = $("input[name=search_enabled]:checked").val();

            var q_identity = identity === "" ? "" : "identity = " + identity + ";";
            var q_name = name === "" ? "" : "name = " + name + ";";
            var q_types = types === "" ? "" : "TYPE(party) in (" + types + ");";
            var q_enabled = enabled === "" ? "" : "enabled = " + enabled + ";";
            var q = q_identity + q_name + q_types + q_enabled;
            q = q.substring(0, q.length - 1);
            q = q === "" ? "" : "q_predicates=[" + q + "]";
            return q;
        }
    }
    initSearchForm();

    function reloadData(url) {
        if(url) {
            dataTable.ajax.url(url).load();
        }
        else {
            dataTable.ajax.reload();
        }
        select_all.prop("checked", false);
        select_all_page_checked = [];
    }
});