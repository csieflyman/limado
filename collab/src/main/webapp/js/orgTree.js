$(function() {
    var jstree = $("#jstree1"), partyFormDialog, partyForm = $("#party_form"),
        parentsTable, childrenTable, parentsIdentity = $("#parents_identity"), childrenIdentity = $("#children_identity"),
    // From http://www.whatwg.org/specs/web-apps/current-work/multipage/states-of-the-type-attribute.html#e-mail-state-%28type=email%29
        emailRegex = /^[a-zA-Z0-9.!#$%&'*+\/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/,
        identity = $( "#identity" ),
        name = $( "#name" ),
        email = $( "#email" ),
        enabled = $("#enabled"),
        allFields = $( [] ).add(identity).add( name ).add( email ).add(enabled),
        tips = $( ".validateTips" );

    function initTree() {
        jstree.jstree({
            core : {
                themes : {
                    variant : "large"
                },
                check_callback : checkOperation,
                data : loadData
            },
            checkbox : {
                three_state : false,
                tie_selection : false
            },
            types: {
                organization: {
                    icon : "images/org.png"
                },
                user: {
                    icon : "images/user.png"
                },
                folder : {
                    icon : "images/folder.png"
                },
                default : {}
            },
            dnd : {
                copy : false
            },
            contextmenu : {
                select_node : false,
                items : function(node) {
                    var actionObject = {};
                    if(node.type === "organization") {
                        actionObject.createUser = {
                            "separator_before": false,
                            "separator_after": true,
                            "label": "Create User",
                            "action": function () {
                                showPartyFormDialog(node, "user");
                            }
                        };
                        actionObject.createOrganization = {
                            "separator_before": false,
                            "separator_after": true,
                            "label": "Create Organization",
                            "action": function () {
                                showPartyFormDialog(node, "organization");
                            }
                        };
                    }
                    if(node.type !== "folder") {
                        actionObject.update = {
                            "separator_before": false,
                            "separator_after": true,
                            "label": "Update",
                            "action": function () {
                                showPartyFormDialog(node);
                            }
                        };
                    }
                    return actionObject;
                }
            },
            sort : sortNode,
            plugins : [ "checkbox", "types", "sort", "dnd", "contextmenu"]
        });

        function checkOperation(operation, node, node_parent, node_position, more) {
            // operation can be 'create_node', 'rename_node', 'delete_node', 'move_node' or 'copy_node'
            if(operation === "move_node" && (node_parent.id === "#" || node_parent.type === "user" || node_parent.type === "folder"))
                return false;
            if(["create_node", "delete_node", "move_node"].indexOf(operation) === -1)
                return false;
            return true;
        }

        function sortNode(node1Id, node2Id) {
            var node1 = this.get_node(node1Id);
            var node2 = this.get_node(node2Id);
            if(node1.type !== node2.type) {
                return node1.type === "organization" ? -1 : 1;
            }
            else {
                return node1.text < node2.text ? -1 : 1;
            }
        }

        jstree.on("move_node.jstree", function (e, data) {
            var childId = data.node.id;
            var newParentId = data.parent;
            $.ajax({
                url:"api/v1/organizations/" + newParentId + "/child/" + childId,
                type:"PUT",
                contentType:"application/json",
                success: function(response) {
                    reloadData();
                },
                error: function(xhr) {
                    alert("move party failure: " + xhr.responseText);
                }
            });
        });

        jstree.on("check_node.jstree", function (e, data) {
            if(data.node.type === "folder") {
                jstree.jstree("check_node", data.node.children);
            }
        })
        jstree.on("uncheck_node.jstree", function (e, data) {
            if(data.node.type === "folder") {
                jstree.jstree("uncheck_node", data.node.children);
            }
        })

        function loadData(node, cb) {
            $.ajax({
                url:"api/v1/parties?q_predicates=[TYPE(party) in (User,Organization)]&q_fetchRelations=parents,children",
                type:"GET",
                dataType:"json",
                success: function(response) {
                    var nodes = buildTree(response);
                    cb.call(jstree, nodes);
                },
                error: function(xhr) {
                    alert("fetch party failure: " + xhr.responseText);
                }
            });

            function buildTree(parties) {
                parties.forEach(removeGroupParents);

                var noParentsUsers = getUsersWithoutParents(parties);
                var noParentsAndChildrenOrgs = getOrganizationsWithoutParentsAndChildren(parties);
                var folderNodes = [];
                if(noParentsUsers.length !== 0 || noParentsAndChildrenOrgs.length !== 0) {
                    var root = createDummyRootNode();
                    folderNodes.push(root);
                    if(noParentsUsers.length !== 0) {
                        var userRoot = createDummyUserRootNode();
                        userRoot.parent = root.id;
                        noParentsUsers.forEach(function(party) {
                            party.parent = userRoot.id;
                        });
                        folderNodes.push(userRoot);
                    }
                    if(noParentsAndChildrenOrgs.length !== 0) {
                        var orgRoot = createDummyOrganizationRootNode();
                        orgRoot.parent = root.id;
                        noParentsAndChildrenOrgs.forEach(function(party) {
                            party.parent = orgRoot.id;
                        });
                        folderNodes.push(orgRoot);
                    }
                    parties.forEach(function (party) {
                        if(party.type === "organization" && party.parents.length === 0 && party.children.length !== 0) {
                            party.parent = root.id;
                        }
                    });
                }
                var nodes = parties.map(convertToNode);
                nodes = nodes.concat(folderNodes);
                return nodes;

                function removeGroupParents(party) {
                    party.parents = party.parents.filter(function(parent) {
                        return parent.type !== "group";
                    });
                }

                function getUsersWithoutParents(parties) {
                    return parties.filter(function (party) {
                        return party.type === "user" && party.parents.length === 0;
                    });
                }

                function getOrganizationsWithoutParentsAndChildren(parties) {
                    return parties.filter(function (party) {
                        return party.type === "organization" && party.parents.length === 0 && party.children.length === 0;
                    });
                }

                function createDummyRootNode() {
                    return {
                        id : "_ROOT_",
                        text : "_ROOT_",
                        parent : "#",
                        type : "folder",
                        state : {
                            opened : true,
                            disabled : true,
                            selected : false,
                            checkbox_disabled : true
                        }
                    };
                }
                function createDummyUserRootNode() {
                    return {
                        id : "_USER_",
                        text : "_USER_",
                        parent : "#",
                        type : "folder",
                        state : {
                            opened : false,
                            disabled : false,
                            selected : false
                        }
                    };
                }
                function createDummyOrganizationRootNode() {
                    return {
                        id : "_ORGANIZATION_",
                        text : "_ORGANIZATION_",
                        parent : "#",
                        type : "folder",
                        state : {
                            opened : false,
                            disabled : false,
                            selected : false
                        }
                    };
                }

                function convertToNode(party) {
                    var node = {
                        id : party.id,
                        text : party.name,
                        parent : party.parents[0] === undefined ? "#" : party.parents[0].id
                    };
                    node = $.extend(node, party);
                    node.state = {
                        opened : true,
                        disabled : false,
                        selected : false
                    };
                    delete node.children;
                    delete node.parents;
                    return node;
                }
            }
        }
    }
    initTree();

    function reloadData() {
        jstree.jstree("refresh");
    }

    function initSelectAction() {
        $("#delete").click(function() {
            $.ajax({
                url:"api/v1/parties",
                type:"DELETE",
                data: JSON.stringify(getSelectedNodesId()),
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
                data: JSON.stringify(getSelectedNodesId()),
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
                data: JSON.stringify(getSelectedNodesId()),
                contentType:"application/json",
                success: function() {
                    reloadData();
                },
                error: function(xhr) {
                    alert("disable party failure: " + xhr.responseText);
                }
            });
        });

        function getSelectedNodesId() {
            var nodes = jstree.jstree("get_checked", true);
            var ids = nodes.filter(function (node) {
                return node.type !== "folder";
            }).map(function (node) {
                return node.id;
            });
            return ids;
        }
    }
    initSelectAction();

    function showPartyFormDialog(node, createNodeType) {
        if(createNodeType !== undefined) {
            var parent = node.original;
            if (createNodeType === "user") {
                partyFormDialog.dialog("option", "partyType", "user");
                partyFormDialog.dialog("option", "action", "create");
                partyFormDialog.dialog("option", "title", "Create User");
                initPartyRelationTable("user", [parent], []);
                partyFormDialog.dialog("open");
            }
            else if(createNodeType === "organization") {
                partyFormDialog.dialog("option", "partyType", "organization");
                partyFormDialog.dialog("option", "action", "create");
                partyFormDialog.dialog("option", "title", "Create Organization");
                initPartyRelationTable("organization", [parent], []);
                partyFormDialog.dialog("open");
            }
        }
        else {
            $.ajax({
                url:"api/v1/parties/" + node.id + "?q_fetchRelations=parents,children",
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
        }

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
                            + (isParent ? "TYPE(party) = Organization" : "TYPE(party) in (User,Organization)") + "]",
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
        }
    }

    function initPartyForm() {
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
});