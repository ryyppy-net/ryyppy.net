(function (angular) {
    'use strict';

    angular.module('ryyppy').constant('DrinkOptions', {
        portionSizes: [
            { value: '0.04', text: '4 cl' },
            { value: '0.08', text: '8 cl' },
            { value: '0.12', text: '12 cl' },
            { value: '0.16', text: '16 cl' },
            { value: '0.2', text: '0.2 l' },
            { value: '0.33', text: '0.33 l' },
            { value: '0.4', text: '0.4 l' },
            { value: '0.5', text: '0.5 l' },
            { value: '0.6', text: '0.6 l' },
            { value: '1.0', text: '1.0 l' }
        ],
        defaultPortionSize: '0.33',

        portionAlcoholPercentages: [
            { value: '0.028', text: '2.8%' },
            { value: '0.047', text: '4.7%' },
            { value: '0.052', text: '5.2%' },
            { value: '0.055', text: '5.5%' },
            { value: '0.08', text: '8.0%' },
            { value: '0.12', text: '12.0%' },
            { value: '0.14', text: '14.0%' },
            { value: '0.22', text: '22.0%' },
            { value: '0.30', text: '30.0%' },
            { value: '0.32', text: '32.0%' },
            { value: '0.38', text: '38.0%' },
            { value: '0.40', text: '40.0%' },
            { value: '0.48', text: '48.0%' },
            { value: '0.80', text: '80.0%' }
        ],
        defaultAlcoholPercentage: '0.047',

        // Source: http://en.wikipedia.org/wiki/Toast_(honor)#Worldwide
        salutations: [
            'Gëzuar!',
            'Nazdravlje!',
            'Наздраве!',
            '干杯',
            'Na zdraví!',
            'Terviseks!',
            'Kippis!',
            'Hölökyn kölökyn!',
            'Cul sec!',
            'Saude!',
            'Prost!',
            '乾杯',
            'Sláinte!',
            'Skål!',
            'Slàinte mhath!',
            'Cheers!'
        ]
    });
})(angular);
