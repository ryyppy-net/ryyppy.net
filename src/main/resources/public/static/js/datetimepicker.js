/*
 * DateTimePicker
 * 
 * Usage:
 * var picker = new DateTimePicker('#idOfEmptyDiv');
 * useSomewhereElse(picker.selectedTime);
 */

function DateTimePicker(element) {
    this.element = element;
    this.selectedTime = new Date();
    this.onUpdate = undefined;

    this.appendTemplate();
}

DateTimePicker.prototype.getSelectedTime = function() {
    return this.selectedTime;
}

DateTimePicker.prototype.daysInMonth = function(month, year) {
    var d = new Date(year, month, 0);
    return d.getDate();
}

DateTimePicker.prototype.showSelectedTime = function() {
    $('#dateDay').val(this.selectedTime.getDate());
    $('#dateMonth').val(this.selectedTime.getMonth() + 1);
    $('#dateYear').val(this.selectedTime.getFullYear());

    $('#dateHour').val(this.selectedTime.getHours());
    $('#dateMinute').val(this.selectedTime.getMinutes());
}

DateTimePicker.prototype.adjustWidths = function() {
    var widest = 0;
    $('td', $(this.element)).each(function(index) {
        widest = Math.max(widest, $(this).width());
    }).width(widest);
}

DateTimePicker.prototype.appendTemplate = function() {
    $('#datetimepickerTemplate').tmpl(undefined).appendTo(this.element);

    this.adjustWidths();
    this.showSelectedTime();
    this.initializeButtons();
    this.initializeHandlers();
}

DateTimePicker.prototype.initializeHandlers = function() {
    var that = this;

    $('#dateDay').change(function() {
        var newValue = $(this).val();
        if (newValue >= 1 && newValue <= that.daysInMonth(that.selectedTime.getMonth(), that.selectedTime.getYear()) && !that.formDateIsInTheFuture())
            that.selectedTime.setDate(newValue);
        that.showSelectedTime();
    });

    $('#dateMonth').change(function() {
        var newValue = $(this).val();
        if (newValue >= 1 && newValue <= 12 && !that.formDateIsInTheFuture())
            that.selectedTime.setDate(newValue);
        that.showSelectedTime();
    });

    $('#dateYear').change(function() {
        var newValue = $(this).val();
        if (!that.formDateIsInTheFuture())
            that.selectedTime.setYear(newValue);
        that.showSelectedTime();
    });

    $('#dateHour').change(function() {
        var newValue = $(this).val();
        if (newValue >= 1 && newValue <= 23 && !that.formDateIsInTheFuture())
            that.selectedTime.setHours(newValue);
        that.showSelectedTime();
    });

    $('#dateMinute').change(function() {
        var newValue = $(this).val();
        if (newValue >= 0 && newValue <= 59 && !that.formDateIsInTheFuture())
            that.selectedTime.setMinutes(newValue);
        that.showSelectedTime();
    });
}

DateTimePicker.prototype.getDateFromFormValues = function() {
    var formDate = new Date();
    formDate.setDate($('#dateDay').val());
    formDate.setMonth($('#dateMonth').val() - 1);
    formDate.setYear($('#dateYear').val());
    formDate.setHours($('#dateHour').val());
    formDate.setMinutes($('#dateMinute').val());
    return formDate;
}

DateTimePicker.prototype.formDateIsInTheFuture = function() {
    return this.dateIsInTheFuture(this.getDateFromFormValues());
}

DateTimePicker.prototype.dateIsInTheFuture = function(d) {
    return d > (new Date());
}

DateTimePicker.prototype.manipulateDate = function(event, manipulationFunction) {
    event.preventDefault();
    if (typeof manipulationFunction !== 'undefined')
        manipulationFunction();
    if (typeof this.onUpdate !== 'undefined')
        this.onUpdate();
    this.showSelectedTime();
    return false;
}

DateTimePicker.prototype.initializeButtons = function() {
    var that = this;

    $('#increaseDays').click(function(event) {
        return that.manipulateDate(event, function() {
            var dateCandidate = that.getDateCandidate();
            dateCandidate.setDate(that.selectedTime.getDate() + 1);
            if (!that.dateIsInTheFuture(dateCandidate))
                that.selectedTime = dateCandidate;
        });
    });

    $('#decreaseDays').click(function(event) {
        return that.manipulateDate(event, function() {
            that.selectedTime.setDate(that.selectedTime.getDate() - 1);
        });
    });

    $('#increaseMonths').click(function(event) {
        return that.manipulateDate(event, function() {
            var dateCandidate = that.getDateCandidate();
            dateCandidate.setMonth(that.selectedTime.getMonth() + 1);
            if (!that.dateIsInTheFuture(dateCandidate))
                that.selectedTime = dateCandidate;
        });
    });

    $('#decreaseMonths').click(function(event) {
        return that.manipulateDate(event, function() {
            that.selectedTime.setMonth(that.selectedTime.getMonth() - 1);
        });
    });

    $('#increaseYears').click(function(event) {
        return that.manipulateDate(event, function() {
            var dateCandidate = that.getDateCandidate();
            dateCandidate.setYear(that.selectedTime.getFullYear() + 1);
            if (!that.dateIsInTheFuture(dateCandidate))
                that.selectedTime = dateCandidate;
        });
    });

    $('#decreaseYears').click(function(event) {
        return that.manipulateDate(event, function() {
            that.selectedTime.setYear(that.selectedTime.getFullYear() - 1);
        });
    });

    $('#increaseHours').click(function(event) {
        return that.manipulateDate(event, function() {
            var dateCandidate = that.getDateCandidate();
            dateCandidate.setHours(that.selectedTime.getHours() + 1);
            if (!that.dateIsInTheFuture(dateCandidate))
                that.selectedTime = dateCandidate;
        });
    });

    $('#decreaseHours').click(function(event) {
        return that.manipulateDate(event, function() {
            that.selectedTime.setHours(that.selectedTime.getHours() - 1);
        });
    });

    $('#increaseMinutes').click(function(event) {
        return that.manipulateDate(event, function() {
            var dateCandidate = that.getDateCandidate();
            dateCandidate.setMinutes(that.selectedTime.getMinutes() + 1);
            if (!that.dateIsInTheFuture(dateCandidate))
                that.selectedTime = dateCandidate;
        });
    });

    $('#decreaseMinutes').click(function(event) {
        return that.manipulateDate(event, function() {
            that.selectedTime.setMinutes(that.selectedTime.getMinutes() - 1);
        });
    });
}

DateTimePicker.prototype.getDateCandidate = function() {
    var candidate = new Date();
    candidate.setTime(this.selectedTime.valueOf());
    return candidate;
}
