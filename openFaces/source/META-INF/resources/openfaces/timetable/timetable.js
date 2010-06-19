/*
 * OpenFaces - JSF Component Library 2.0
 * Copyright (C) 2007-2010, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */

O$.Timetable = {};

O$.Timetable._initEventEditorDialog = function(dayTableId, dialogId, createEventCaption, editEventCaption, centered) {
  var dayTable = O$(dayTableId);
  var dialog = O$(dialogId);
  dayTable._eventEditor = dialog;
  dialog._dayTable = dayTable;

  dialog._nameField = O$.byIdOrName(dialog.id + "--nameField");
  dialog._resourceField = O$.byIdOrName(dialog.id + "--resourceField");
  dialog._startDateField = O$.byIdOrName(dialog.id + "--startDateField");
  dialog._endDateField = O$.byIdOrName(dialog.id + "--endDateField");
  dialog._startTimeField = O$.byIdOrName(dialog.id + "--startTimeField");
  dialog._endTimeField = O$.byIdOrName(dialog.id + "--endTimeField");
  dialog._colorField = O$.byIdOrName(dialog.id + "--colorField");
  dialog._color = "";
  dialog._descriptionArea = O$.byIdOrName(dialog.id + "--descriptionArea");
  dialog._okButton = O$.byIdOrName(dialog.id + "--okButton");
  dialog._cancelButton = O$.byIdOrName(dialog.id + "--cancelButton");
  dialog._deleteButton = O$.byIdOrName(dialog.id + "--deleteButton");

  var fixedDurationMode = !dialog._endDateField;

  function okByEnter(fld) {
    if (!fld)
      return;
    if (fld instanceof Array) {
      for (var i in fld) {
        var entry = fld[i];
        okByEnter(entry);
      }
      return;
    }
    fld.onkeydown = function(e) {
      var evt = O$.getEvent(e);
      if (evt.keyCode != 13)
        return;
      if (this.nodeName.toLowerCase() == "textarea") {
        if (!evt.ctrlKey)
          return;
      }
      dialog._okButton.onclick(e);
    };
  }

  okByEnter([dialog._nameField, dialog._resourceField, dialog._startDateField, dialog._endDateField,
    dialog._startTimeField, dialog._endTimeField, dialog._colorField, dialog._descriptionArea]);

  function getFieldText(field) {
    if (field.getValue)
      return field.getValue();
    return field.value;
  }

  function setFieldText(field, text) {
    if (field.setValue)
      field.setValue(text);
    else
      field.value = text;
  }

  dialog.run = function(event, mode) {
    this._event = event;
    setFieldText(this._nameField, event.name);
    var resource = dayTable._getResourceForEvent(event);
    if (dialog._resourceField)
      dialog._resourceField.setValue(resource ? resource.name : "");
    this._startDateField.setSelectedDate(event.start);
    if (this._endDateField)
      this._endDateField.setSelectedDate(event.end);
    var duration = event.end.getTime() - event.start.getTime();
    setFieldText(this._startTimeField, O$.formatTime(event.start));
    if (this._endTimeField)
      setFieldText(this._endTimeField, O$.formatTime(event.end));
    this._color = event.color;
    setFieldText(this._descriptionArea, event.description);
    this._deleteButton.style.visibility = mode == "update" ? "visible" : "hidden";
    O$.removeAllChildNodes(this._captionContent);
    this._captionContent.appendChild(document.createTextNode(mode == "update"
            ? editEventCaption
            : createEventCaption));

    this._okPressed = false;
    this._okButton.onclick = function(e) {
      this._okProcessed = true;
      O$.breakEvent(e);
      event.name = getFieldText(dialog._nameField);
      var startDate = dialog._startDateField.getSelectedDate();
      if (!startDate) {
        dialog._startDateField.focus();
        return;
      }
      O$.parseTime(getFieldText(dialog._startTimeField), startDate);
      var endDate = dialog._endDateField ? dialog._endDateField.getSelectedDate() : null;
      if (dialog._endTimeField) {
        if (!endDate) {
          dialog._endDateField.focus();
          return;
        }
        O$.parseTime(getFieldText(dialog._endTimeField), endDate);
      }
      if (!startDate || isNaN(startDate)) {
        dialog._startTimeField.focus();
        return;
      }
      if (!fixedDurationMode && (!endDate || isNaN(endDate))) {
        dialog._endTimeField.focus();
        return;
      }
      event.setStart(startDate);
      if (fixedDurationMode) {
        // fixed duration mode
        endDate = new Date();
        endDate.setTime(startDate.getTime() + duration);
      }
      event.setEnd(endDate);
      if (dialog._resourceField)
        event.resourceId = dayTable._idsByResourceNames[dialog._resourceField.getValue()];
      event.color = dialog._color ? dialog._color : "";
      event.description = getFieldText(dialog._descriptionArea);
      dialog.hide();
      if (mode == "create")
        dayTable.addEvent(event);
      else
        dayTable.updateEvent(event);
    };

    this._cancelButton.onclick = function(e) {
      O$.breakEvent(e);
      dialog.hide();
      if (mode == "create")
        dayTable.cancelEventCreation(event);
    };

    this._deleteButton.onclick = function(e) {
      O$.breakEvent(e);
      dialog.hide();
      if (mode == "update")
        dayTable.deleteEvent(event);
    };

    this.onhide = function() {
      if (!this._okProcessed && mode == "create")
        dayTable.cancelEventCreation(event);
      if (dialog._textareaHeightUpdateInterval)
        clearInterval(dialog._textareaHeightUpdateInterval);
    };

    if (event.parts){
      for (var i = 0; i < event.parts.length; i++) {
        if (event.parts[i].mainElement)
          O$.correctElementZIndex(this, event.parts[i].mainElement, 5);
      }
    }
    if (centered)
      this.showCentered();
    else
      this.show();

    function adjustTextareaHeight() {
      var size = O$.getElementSize(dialog._descriptionArea.parentNode);
      O$.setElementSize(dialog._descriptionArea, size);
    }

    if (O$.isExplorer() || O$.isOpera()) {
      if (dialog._descriptionArea.style.position != "absolute") {
        dialog._descriptionArea.style.position = "absolute";
        var div = document.createElement("div");
        div.style.height = "100%";
        dialog._descriptionArea.parentNode.appendChild(div);
      }
      adjustTextareaHeight();
      dialog._textareaHeightUpdateInterval = setInterval(adjustTextareaHeight, 50);
    } else
      dialog._textareaHeightUpdateInterval = null;

  };

};

O$.Timetable._initEventEditorPage = function(dayTableId, thisComponentId, actionDeclared, url, modeParamName,
                                   eventIdParamName, eventStartParamName, eventEndParamName, resourceIdParamName) {
  var dayTable = O$(dayTableId);
  var thisComponent = O$(thisComponentId);
  dayTable._eventEditor = thisComponent;
  thisComponent.run = function(event, mode) {
    if (actionDeclared) {
      var params = (mode == "create") ?
                   [
                     [thisComponentId + "::action", "action"],
                     [modeParamName, mode],
                     [eventStartParamName, event.startStr],
                     [eventEndParamName, event.endStr],
                     [resourceIdParamName, event.resourceId]
                   ] :
                   [
                     [thisComponentId + "::action", "action"],
                     [modeParamName, mode],
                     [eventIdParamName, event.id]
                   ];
      O$.submitFormWithAdditionalParams(dayTable, params);
      return;
    }
    var newPageUrl = url + "?" + modeParamName + "=" + mode + "&";
    if (mode == "create") {
      newPageUrl += eventStartParamName + "=" + encodeURIComponent(event.startStr) + "&";
      newPageUrl += eventEndParamName + "=" + encodeURIComponent(event.endStr) + "&";
      newPageUrl += resourceIdParamName + "=" + encodeURIComponent(event.resourceId);
    } else if (mode == "update") {
      newPageUrl += eventIdParamName + "=" + encodeURIComponent(event.id);
    }
    window.location = newPageUrl;
  };
};

O$.Timetable._initCustomEventEditor = function(dayTableId, thisComponentId, oncreate, onedit) {
  var dayTable = O$(dayTableId);
  var thisComponent = O$(thisComponentId);
  dayTable._eventEditor = thisComponent;
  thisComponent.run = function(event, mode) {
    if (mode == "create")
      oncreate(dayTable, event);
    else
      onedit(dayTable, event);
  };

};

O$.Timetable._initEvent = function(event) {
  event.setStart = function(asDate, asString) {
    if (asDate) {
      event.start = asDate;
      event.startStr = O$.formatDateTime(asDate);
    } else
      if (asString) {
        event.startStr = asString;
        event.start = O$.parseDateTime(asString);
      } else
        throw "event.setStart: either asDate parameter, or asTime parameter should be specified";
  };
  event.setEnd = function(asDate, asString) {
    if (asDate) {
      event.end = asDate;
      event.endStr = O$.formatDateTime(asDate);
    } else
      if (asString) {
        event.endStr = asString;
        event.end = O$.parseDateTime(asString);
      } else
        throw "event.setEnd: either asDate parameter, or asTime parameter should be specified";
  };
  event._copyFrom = function(otherEvent) {
    this.id = otherEvent.id;
    this.setStart(otherEvent.start, otherEvent.startStr);
    this.setEnd(otherEvent.end, otherEvent.endStr);
    this.name = otherEvent.name;
    this.description = otherEvent.description;
    this.resourceId = otherEvent.resourceId;
    this.color = otherEvent.color;
  };
  
  event._scrollIntoView = function() {
    /*    return; //todo: finish auto-scrolling functionality
     var dayTable = event.mainElement._dayTable;
     var scrollingOccured = O$.scrollElementIntoView(event.mainElement, dayTable._getScrollingCache());
     if (scrollingOccured)
     dayTable._resetScrollingCache();*/
  };
  if (event.start || event.startStr)
    event.setStart(event.start, event.startStr);
  if (event.end || event.endStr)
    event.setEnd(event.end, event.endStr);
};

O$.Timetable._initEventPreview = function(eventPreviewId, dayTableId, showingDelay, popupClass,
                                eventNameClass, eventDescriptionClass,
                                horizontalAlignment, verticalAlignment, horizontalDistance, verticalDistance) {
  var eventPreview = O$(eventPreviewId);
  var popupLayer = O$(eventPreviewId + "--popupLayer");
  O$.appendClassNames(popupLayer, [popupClass]);

  eventNameClass = O$.combineClassNames(["o_timetableEventName", eventNameClass]);
  eventDescriptionClass = O$.combineClassNames(["o_timetableEventDescription", eventDescriptionClass]);

  eventPreview._showingDelay = showingDelay;

  eventPreview.showForEvent = function(event) {
    var dayTable = O$(dayTableId);
    var popupParent = O$.getDefaultAbsolutePositionParent();
    if (popupLayer.parentNode != popupParent) {
      popupParent.appendChild(popupLayer);
    }
    O$.correctElementZIndex(popupLayer, dayTable);

    var oldSpans;
    while ((oldSpans = popupLayer.getElementsByTagName("span")).length > 0) {
      var span = oldSpans[0];
      span.parentNode.removeChild(span);
    }

    var nameElement = document.createElement("span");
    nameElement.className = eventNameClass;
    popupLayer.appendChild(nameElement);

    var descriptionElement = document.createElement("span");
    descriptionElement.className = eventDescriptionClass;
    popupLayer.appendChild(descriptionElement);

    O$.setInnerText(nameElement, event.name, dayTable._escapeEventNames);
    O$.setInnerText(descriptionElement, event.description, dayTable._escapeEventDescriptions);

    var mainElement = event.parts[0].mainElement;
    if (!mainElement)
      popupLayer.showCentered();
    else
      popupLayer.showByElement(mainElement,
              horizontalAlignment, verticalAlignment, horizontalDistance, verticalDistance);
  };

  eventPreview.hide = function() {
    popupLayer.hide();
  };
};


O$.Timetable._initEventActionBar = function(actionBarId, dayTableId, backgroundIntensity, userSpecifiedClass, actions,
                                  actionRolloverIntensity, actionPressedIntensity) {
  var actionBar = O$(actionBarId);
  if (!actionBar) {
    var initArgs = arguments;
    // postpone initialization to avoid FF2 failure of finding actionBar element in some cases
    setTimeout(function() {
      O$.Timetable._initEventActionBar.apply(null, initArgs);
    }, 100);
    return;
  }

  actionBar._inactiveSegmentIntensity = backgroundIntensity;
  actionBar._userSpecifiedClass = userSpecifiedClass;
  actionBar.className = O$.combineClassNames(["o_eventActionBar", userSpecifiedClass]);

  var actionsTable = document.createElement("table");
  actionsTable.cellSpacing = "0";
  actionsTable.cellPadding = "0";
  actionsTable.border = "0";
  actionsTable.style.fontSize = "0";
  var tbody = document.createElement("tbody");
  actionsTable.appendChild(tbody);
  var tr = document.createElement("tr");
  tbody.appendChild(tr);
  for (var i = 0, count = actions.length; i < count; i++) {
    var action = actions[i];
    var cell = document.createElement("td");
    action._cell = cell;
    cell._index = i;
    cell._action = action;
    cell._image = image;

    tr.appendChild(cell);
    cell.vAlign = "middle";
    cell.align = "center";
    cell.id = action.id;
    cell.className = action.style[0];
    var image = document.createElement("img");
    image.src = action.image[0];
    if (action.hint)
      image.title = action.hint;
    cell.appendChild(image);
    O$.assignEvents(cell, action, true);
    O$.preloadImage(action.image[0]);
    O$.preloadImage(action.image[1]);
    O$.preloadImage(action.image[2]);
    cell._userClickHandler = cell.onclick;
    cell.onmousedown = function() {
      this._timetableEvent = actionBar._event ? actionBar._event : actionBar._lastEditedEvent;
      this._timetableEventPart = actionBar._part ? actionBar._part : actionBar._lastEditedPart;
      this._dayTable = O$(dayTableId);
      if (this._timetableEventPart.mainElement._bringToFront) {
        this._timetableEventPart.mainElement._bringToFront();
      }
    };
    cell.onclick = function(e) {
      e = O$.getEvent(e);
      e._timetableEvent = this._timetableEvent;
      e._dayTable = this._dayTable;
      if (this._userClickHandler) {
        if (this._userClickHandler(e) === false || e.returnValue === false)
          return;
      }

      var eventId = this._timetableEvent.id;
      O$.setHiddenField(this._dayTable, actionBarId + "::" + this._index, eventId);
      O$.submitEnclosingForm(this._dayTable);
    };
    function setupStateHighlighting(cell) {
      cell._mouseState = O$.setupHoverAndPressStateFunction(cell, function(mouseInside, pressed) {
        cell._mouseInside = mouseInside;
        cell._pressed = pressed;
        cell._update();
      });
    }

    setupStateHighlighting(cell);
    cell._update = function() {
      var mouseInside = this._mouseInside;
      var pressed = this._pressed;
      O$.setStyleMappings(this, {
        _rolloverStyle: mouseInside ? this._action.style[1] : null,
        _pressedStyle: pressed ? this._action.style[2] : null});
      var userSpecifiedBackground = O$.getStyleClassProperty(this.className, "background-color");
      if (!userSpecifiedBackground) {
        var event = actionBar._event ? actionBar._event : actionBar._lastEditedEvent;
        var part = actionBar._part ? actionBar._part : actionBar._lastEditedPart;
        var intensity = pressed ? actionPressedIntensity : mouseInside ? actionRolloverIntensity : backgroundIntensity;
        this.style.backgroundColor = O$.blendColors(part.mainElement._color, "#ffffff", 1 - intensity);
      } else
        this.style.backgroundColor = "";

      var imageUrl = action.image[0];
      if (pressed) {
        if (action.image[2])
          imageUrl = action.image[2];
      } else if (mouseInside) {
        if (action.image[1])
          imageUrl = action.image[1];
      }
      this._image = imageUrl;
    };
  }
  actionsTable.onclick = function(e) {
    O$.breakEvent(e); // avoid passing event to the absoluteElementsParentNode
  };
  actionBar._actionsArea = actionsTable;
  actionBar._actionsArea.style.position = "absolute";
  actionBar._actionsArea.style.visibility = "hidden";
  actionsTable._getHeight = function() {
    if (!this._height) {
      this._height = O$.getElementSize(this).height;
    }
    return this._height;
  };
  actionsTable._getWidth = function() {
    if (!this._width) {
      this._width = O$.getElementSize(this).width;
    }
    return this._width;
  };
  actionBar.appendChild(actionsTable);

  actionBar._update = function() {
    actionBar._actionsArea._updatePos();
    for (var i = 0, count = actions.length; i < count; i++) {
      var action = actions[i];
      var cell = action._cell;
      cell._update();
    }
  };

  O$.setupHoverStateFunction(actionBar._actionsArea, function(mouseInside) {
    actionBar._actionsArea._mouseInside = mouseInside;
    if (actionBar._event)
      O$.invokeFunctionAfterDelay(actionBar._event._updateRolloverState, O$.EVENT_ROLLOVER_STATE_UPDATE_TIMEOUT);
  });


  actionBar._actionsArea._updatePos = function() {
    var dayTable = O$(dayTableId);
    var actionBarSize = O$.getElementSize(actionBar);
    var actionsAreaSize = O$.getElementSize(this);
    this.style.top = "0px";
    this.style.left = actionBarSize.width - actionsAreaSize.width + "px";
    this.style.height = actionBarSize.height + "px";

    O$.correctElementZIndex(this, dayTable);
  };
};

/*
 This function is invoked from the "delete event" action button. Don't invoke this function directly.
 */
O$.Timetable._deleteCurrentTimetableEvent = function(event) {
  var e = O$.getEvent(event);
  var timetableEvent = e._timetableEvent;
  e._dayTable.deleteEvent(timetableEvent);
  e.returnValue = false;
};

O$.Timetable._createEventContentElements = function(eventElement, eventContent, eventContentClasses) {
  eventElement._contentElements = [];

  for (var contentIndex = 0; contentIndex < eventContent.length; contentIndex++) {
    var contentPart = eventContent[contentIndex];
    var defaultContentClass = eventContentClasses[contentPart.type];
    var contentClass = O$.combineClassNames([defaultContentClass, contentPart.style]);
    var contentElement = document.createElement("span");
    if (contentClass) {
      contentElement.className = contentClass;
    }
    contentElement._contentPart = contentPart;
    eventElement.appendChild(document.createTextNode(" "));
    eventElement.appendChild(contentElement);
    eventElement._contentElements.push(contentElement);
  }
};

O$.Timetable._updateEventContentElements = function(eventElement, event, timetable) {
  for (var elementIndex = 0; elementIndex < eventElement._contentElements.length; elementIndex++) {
    var contentElement = eventElement._contentElements[elementIndex];
    if (contentElement._contentPart.type == "name") {
      O$.setInnerText(contentElement, event.name, timetable._escapeEventNames);
    } else if (contentElement._contentPart.type == "description") {
      O$.setInnerText(contentElement, event.description, timetable._escapeEventDescriptions);
    } else if (contentElement._contentPart.type == "resource") {
      var resource = timetable._getResourceForEvent(event);
      var resourceName = resource ? resource.name : "";
      O$.setInnerText(contentElement, resourceName, timetable._escapeEventResources);
    } else if (contentElement._contentPart.type == "time") {
      var timeText = O$.formatTime(event.start) + " - " + O$.formatTime(event.end);
      O$.setInnerText(contentElement, timeText, false);
    } else if (contentElement._contentPart.type == "lineFeed") {
      O$.removeAllChildNodes(contentElement);
      var lineFeed = document.createElement("br");
      contentElement.appendChild(lineFeed);
    }
  }
};

O$.Timetable._findEventById = function(events, id) {
  if (events._cachedEventsByIds) {
    return events._cachedEventsByIds[id];
  }
  events._cachedEventsByIds = {};
  for (var i = 0, count = events.length; i < count; i++) {
    var event = events[i];
    events._cachedEventsByIds[event.id] = id;
    if (event.id == id)
      return event;
  }
  return null;
};

O$.Timetable._LazyLoadedTimetableEvents = function(preloadedEvents, preloadedStartTime, preloadedEndTime) {
  O$.Timetable._PreloadedTimetableEvents.call(this, []);

  this._setEvents = this.setEvents;
  this.setEvents = function(events, preloadedStartTime, preloadedEndTime) {
    this._setEvents(events);
    this._loadedTimeRangeMap = new O$._RangeMap();
    this._loadingTimeRangeMap = new O$._RangeMap();
    if (!(preloadedStartTime instanceof Date))
      preloadedStartTime = preloadedStartTime ? O$.parseDateTime(preloadedStartTime).getTime() : null;
    if (!(preloadedEndTime instanceof Date))
      preloadedEndTime = preloadedEndTime ? O$.parseDateTime(preloadedEndTime).getTime() : null;
    this._loadedTimeRangeMap.addRange(preloadedStartTime, preloadedEndTime);
    this._loadingTimeRangeMap.addRange(preloadedStartTime, preloadedEndTime);
  };
  this.setEvents(preloadedEvents, preloadedStartTime, preloadedEndTime);

  this._getEventsForPeriod_raw = this._getEventsForPeriod;
  this._getEventsForPeriod = function(start, end, eventsLoadedCallback) {
    if (this._loadedTimeRangeMap.isRangeFullyInMap(start.getTime(), end.getTime()) ||
        this._loadingTimeRangeMap.isRangeFullyInMap(start.getTime(), end.getTime()))
      return this._getEventsForPeriod_raw(start, end);

    this._loadingTimeRangeMap.addRange(start.getTime(), end.getTime());
    var thisProvider = this;
    O$.requestComponentPortions(this._timeTableView.id, ["loadEvents"],
            JSON.stringify(
            {startTime: O$.formatDateTime(start), endTime: O$.formatDateTime(end)},
                    ["startTime", "endTime"]),
            function(component, portionName, portionHTML, portionScripts, portionData) {
              var remainingElements = O$.replaceDocumentElements(portionHTML, true);
              if (remainingElements.hasChildNodes())
                thisProvider._timeTableView._hiddenArea.appendChild(remainingElements);
              O$.executeScripts(portionScripts);

              var newEvents = portionData.events;
              thisProvider._loadedTimeRangeMap.addRange(start.getTime(), end.getTime());
              thisProvider._events._cachedEventsByIds = null;
              for (var i = 0, count = newEvents.length; i < count; i++) {
                var newEvent = newEvents[i];
                var existingEvent = O$.Timetable._findEventById(thisProvider._events, newEvent.id);
                if (existingEvent)
                  existingEvent._copyFrom(newEvent);
                else
                  thisProvider.addEvent(newEvent);
              }
              if (eventsLoadedCallback) {
                //        var eventsForPeriod = this._getEventsForPeriod_raw(start, end);
                eventsLoadedCallback();//eventsForPeriod);
              }
            }, function () {
      // todo: revert addition of time range to this._loadingTimeRangeMap
      alert('Error loading timetable events');
    });
    return this._getEventsForPeriod_raw(start, end);
  };
};

O$.Timetable._PreloadedTimetableEvents = function(events) {

  this._getEventsForPeriod = function(start, end) {
    var result = [];
    var startTime = start.getTime();
    var endTime = end.getTime();
    for (var eventIndex = 0, eventCount = this._events.length; eventIndex < eventCount; eventIndex++) {
      var event = this._events[eventIndex];
      if (event.end.getTime() < event.start.getTime())
        continue;
      if (event.end.getTime() <= startTime ||
          event.start.getTime() >= endTime)
        continue;
      result.push(event);
    }
    return result;
  };

  this.setEvents = function(newEvents) {
    this._events = newEvents;
    for (var eventIndex = 0, eventCount = newEvents.length; eventIndex < eventCount; eventIndex++) {
      var event = newEvents[eventIndex];
      O$.Timetable._initEvent(event);
    }
    this._events._cachedEventsByIds = null;
  };

  this.getEventById = function(eventId) {
    for (var i = 0, count = this._events.length; i < count; i++) {
      var evt = this._events[i];
      if (evt.id == eventId)
        return evt;
    }
    return null;
  };

  this.addEvent = function(event) {
    if (this._events._cachedEventsByIds && event.id)
      this._events._cachedEventsByIds[event.id] = event;
    O$.Timetable._initEvent(event);
    this._events.push(event);
  };
  this.deleteEvent = function(event) {
    if (this._events._cachedEventsByIds && event.id)
      this._events._cachedEventsByIds[event.id] = undefined;

    var eventIndex = O$.findValueInArray(event, this._events);
    this._events.splice(eventIndex, 1);
  };

  this.setEvents(events);
};

