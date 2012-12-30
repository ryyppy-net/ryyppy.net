basePath = '../';

files = [
  JASMINE,
  JASMINE_ADAPTER,
  'app/js/lib/angular/angular.js',
  'app/js/lib/angular/angular-*.js',
  'test/lib/angular/angular-mocks.js',
  'app/js/**/*.js',
  'test/unit/**/*.js'
];

exclude = [
    'app/js/lib/flot/*.js',
    'app/js/lib/jquery/*.js',
    'app/js/lib/pnotify/*.js'
];

autoWatch = true;

browsers = ['Chrome'];

junitReporter = {
  outputFile: 'test_out/unit.xml',
  suite: 'unit'
};
