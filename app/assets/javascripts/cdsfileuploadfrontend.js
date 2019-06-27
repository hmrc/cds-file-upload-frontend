/** JQuery Validation custom validators **/

$.validator.addMethod('file-extensions', function (value, element, param) {
    return this.optional(element) || param.split(",").some(function(ext) {
        return element.files[0].name.endsWith(ext) })
}, 'File must have an ext of {0}');

$.validator.addMethod('filesize', function (value, element, param) {
    return this.optional(element) || (element.files[0].size / 1024 / 1024 <= param)
}, 'File size must be less than {0}');