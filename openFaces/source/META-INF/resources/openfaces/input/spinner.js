/*
 * OpenFaces - JSF Component Library 3.0
 * Copyright (C) 2007-2012, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */
//TODO: do not remove until normal maskEdit will solve this issue to tests the behaviour notice that this solution works only inside chrome
O$.SpinMask = {
  _init: function(inputId, mask) {
    mask = {
      emptySymbols: '',
      format: '',
      regex: /^-?\d+(\.\d+)?$/
    }
    var inputField = O$(inputId);
    inputField.defaultValue = mask.format;
    if (inputField.value.length<0){
      inputField.value = mask.format;
    }
    O$.extend(inputField,{
      isCharacterKey: function(key) {
        return ( key >= 31 && key < 128 );
      },
      isControlKey : function(key) {
        switch(key) {
          case 8:
            return true;
            break;
          case 36:
            return true;
            break;
          case 35:
            return true;
            break;
          case 37:
            return true;
          case 38:
            return true;
            break;
          case 39:
            return true;
            break;
          case 40:
            return true;
            break;
          case 46:
            return true;
            break;
          default:
            return false;
        }
      },
      //TODO: chrome only solution add support of other browsers
      onkeypress: function (e){
        var key = e.keyCode;
        if (this.isControlKey(key)) {

        }else if (this.isCharacterKey(key)) {
          var ch = String.fromCharCode( e.charCode);
          var str = this.value;
          var start = this.selectionStart;
          var end = this.selectionEnd;
          str = str.substring(0, start) + ch + str.substring(end);
          var pos = str.length;
          if ( mask.regex.test(str+"0") && (pos <= mask.format.length || mask.format.length == 0) ) {
            if ( mask.emptySymbols.indexOf(mask.format.charAt(pos - 1)) == -1 ) {
              str = this.value + mask.format.charAt(pos - 1) + ch;
            }
            this.value = str;
            var cursorPosition = start + ch.length;
            this.setSelectionRange(cursorPosition, cursorPosition);
          }
          return false;
        }
      }
    })

  }
};


O$.Spinner = {
  _init: function(spinnerId,
                  minValue,
                  maxValue,
                  step,
                  cycled,
                  buttonClass,
                  rolloverButtonClass,
                  pressedButtonClass,
                  disabled,
                  required,
                  onchange,
                  formatOptions) {
    var spinner = O$.initComponent(spinnerId, null, {
      _disabled: null,
      _prevValue: O$(spinnerId)._initialText,
      setDisabled: function(disabled) {
        if (this._disabled == disabled) return;

        this._disabled = disabled;

        this._setFieldDisabled(disabled);

        if (disabled){
          this._unSetupButtonAndFieldEvents();
        }else{
          this._setupButtonAndFieldEvents();
        }
      },

      getDisabled: function() {
        return this._disabled;
      },

      getValue: function() {
        if (!spinner._field)
          return null;
        var value = O$.Dojo.Number.parse(spinner._field.value, formatOptions);
        if (isNaN(value)) {
          value = parseFloat(spinner._field.value, 10);
        }
        return !isNaN(value) ? value : null;
      },

      setValue: function(value, silent) {
        var newValue = value == null || isNaN(value) ? "" : value;
        if (newValue === "") {
          spinner._field.value = required ? spinner._prevValue : "";
        } else {
          if (maxValue != null && value > maxValue)
            newValue = maxValue;
          if (minValue != null && value < minValue)
            newValue = minValue;
          spinner._field.value = O$.Dojo.Number.format(newValue, formatOptions);
        }
        if (!silent && newValue != spinner._prevValue) {
          notifyOfInputChanges(spinner);
        }
        spinner._prevValue = spinner.getValue();
      },

      increaseValue: function() {
        var value = spinner.getValue();
        if (!value && value != 0) {
          spinner.setValue(minValue != null ? minValue : 0, true);
        } else if ((maxValue == null || value + step <= maxValue) && (minValue == null || value + step >= minValue)) {
          spinner.setValue(value + step, true);
        } else {
          if (cycled) {
            if (minValue != null)
              spinner.setValue(minValue, true);
          } else {
            if (maxValue != null && value > maxValue)
              spinner.setValue(maxValue, true);
            if (minValue != null && value < minValue)
              spinner.setValue(minValue, true);
          }
        }
        if (value != spinner.getValue()) {
          notifyOfInputChanges(spinner);
        }
      },

      decreaseValue: function() {
        var value = spinner.getValue();
        if (!value && value != 0) {
          spinner.setValue(minValue != null ? minValue : 0, true);
        } else if ((maxValue == null || value - step <= maxValue) && (minValue == null || value - step >= minValue)) {
          spinner.setValue(value - step, true);
        } else {
          if (cycled) {
            if (maxValue != null)
              spinner.setValue(maxValue, true);
          } else {
            if (maxValue != null && value > maxValue)
              spinner.setValue(maxValue, true);
            if (minValue != null && value < minValue)
              spinner.setValue(minValue, true);
          }
        }
        if (value != spinner.getValue()) {
          notifyOfInputChanges(spinner);
        }
      }

    });


    O$.extend(spinner,{
      _setupButtonAndFieldEvents: function(){
        field.onkeypress = function(e) {
          var valueBefore = spinner.getValue();
          setTimeout(function() {
            var valueAfter = spinner.getValue();
            if (valueBefore == valueAfter)
              return;
            notifyOfInputChanges(spinner);
          }, 1);
          O$.stopEvent(e);
        };

        field.onblur = function(e) {
          checkValueForBounds(spinner);
        };

        increaseButton.ondragstart = function(e) {
          O$.cancelEvent(e);
        };
        decreaseButton.ondragstart = function(e) {
          O$.cancelEvent(e);
        };

        increaseButton.onselectstart = function (e) {
          O$.cancelEvent(e);
        };
        decreaseButton.onselectstart = function (e) {
          O$.cancelEvent(e);
        };
        O$.setupHoverAndPressStateFunction(increaseButton, function(mouseInside, pressed) {
          O$.setStyleMappings(increaseButton, {
            rollover: mouseInside ? rolloverButtonClass : null,
            pressed: pressed ? pressedButtonClass : null
          });
        });
        O$.setupHoverAndPressStateFunction(decreaseButton, function(mouseInside, pressed) {
          O$.setStyleMappings(decreaseButton, {
            rollover: mouseInside ? rolloverButtonClass : null,
            pressed: pressed ? pressedButtonClass : null
          });
        });
        increaseButton.onmousedown = function(e) {
          setTimeout(function() {
            spinner._field.focus();
          }, 1);
          spinner.increaseValue();
          O$.cancelEvent(e);
        };
        decreaseButton.onmousedown = function(e) {
          setTimeout(function() {
            spinner._field.focus();
          }, 1);

          spinner.decreaseValue();
          O$.cancelEvent(e);
        };
        field._oldInkeydown = field.onkeydown;

        field.onkeydown = function(e) {
          var evt = O$.getEvent(e);
          if (!evt) return;
          var keyCode = evt.keyCode;
          if (keyCode == 38) { // Up key
            increaseButton.onmousedown(e);
          } else if (keyCode == 40) { // Down key
            decreaseButton.onmousedown(e);
          }
          if (field._oldInkeydown)
            field._oldInkeydown(e);
        };
      },
      _unSetupButtonAndFieldEvents: function(){
        field.onkeypress = function(e) {
          O$.stopEvent(e);
        };

        field.onblur = function() {
        };

        increaseButton.ondragstart = function(e) {
          O$.cancelEvent(e);
        };
        decreaseButton.ondragstart = function(e) {
          O$.cancelEvent(e);
        };

        increaseButton.onselectstart = function (e) {
          O$.cancelEvent(e);
        };
        decreaseButton.onselectstart = function (e) {
          O$.cancelEvent(e);
        };
        O$.setupHoverAndPressStateFunction(increaseButton, function(){});
        O$.setupHoverAndPressStateFunction(decreaseButton, function(){});
        increaseButton.onmousedown = function(e) {
          O$.cancelEvent(e);
        };
        decreaseButton.onmousedown = function(e) {
          O$.cancelEvent(e);
        };
        field.onkeydown = field._oldInkeydown;
      }
    });

    var increaseButton = O$(spinnerId + "::increase_button");
    var decreaseButton = O$(spinnerId + "::decrease_button");
    var field = spinner._field;

    spinner.setDisabled(disabled);
    var mask = {
      emptySymbols: '',
      format: '',
      regex: /^-?\d+(\.\d+)?$/
    }
    O$.SpinMask._init(spinner._field.id,mask);



    var buttonsTable = O$.getParentNode(increaseButton, "table");
    if (O$.isExplorer() && O$.isStrictMode()) {
      var buttonContainer = buttonsTable.parentNode;
      buttonContainer.style.position = "relative";
      buttonsTable.style.position = "absolute";
      buttonsTable.style.top = "0";
      buttonsTable.style.left = "0";
      var buttonWidth = O$.calculateNumericCSSValue(O$.getStyleClassProperty(buttonClass, "width"));
      buttonContainer.style.width = buttonWidth;
      for (var i = 0, count = buttonContainer.parentNode.childNodes.length; i < count; i++) {
        var node = buttonContainer.parentNode.childNodes[i];
        if (node != buttonContainer && node.nodeName.toLowerCase() == "td")
          node.style.width = "";
      }
    }

    if (increaseButton && decreaseButton) {
      increaseButton.className = buttonClass;
      decreaseButton.className = buttonClass;
    }

    if (onchange) {
      spinner._onchange = function() {
        var event = O$.createEvent("change");
        eval(onchange);
      };
    }

    formatOptions = formatOptions || {};
    if (formatOptions.type && formatOptions.type == "number") {
      formatOptions.type = "decimal";
    }



    decreaseButton.ondblclick = O$.repeatClickOnDblclick;
    increaseButton.ondblclick = O$.repeatClickOnDblclick;

    if (spinner._containerClass != spinner._rolloverContainerClass) {
      O$.addMouseOverListener(spinner, function() {
        if (spinner._containerClass != spinner._rolloverContainerClass)
          spinner.className = spinner._rolloverContainerClass;
        if (spinner._fieldClass != spinner._rolloverFieldClass)
          field.className = spinner._rolloverFieldClass;
        O$.repaintAreaForOpera(spinner, true);
      });
      O$.addMouseOutListener(spinner, function() {
        if (spinner._containerClass != spinner._rolloverContainerClass)
          spinner.className = spinner._containerClass;
        if (spinner._fieldClass != spinner._rolloverFieldClass)
          field.className = spinner._fieldClass;
        O$.repaintAreaForOpera(spinner, true);
      });
    }

    function notifyOfInputChanges(spinner) {
      if (spinner._onchange)
        spinner._onchange(O$.createEvent("change"));
    }

    function checkValueForBounds(spinner) {
      var value = spinner.getValue();
      if (value != null) {
        if (minValue != null && value < minValue) {
          spinner.setValue(minValue);
        } else
        if (maxValue != null && value > maxValue) {
          spinner.setValue(maxValue);
        } else {
          spinner.setValue(value, true);
        }
      } else {
        spinner.setValue("", false);
      }
    }

  }
};