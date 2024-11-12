(() => {
    const messages = (key, interpolation) => {
        const message = window.messages[key]
        return message.replace("{0}", interpolation)
    }
    const originalPageTitle = document.title
    const errorSummary = document.getElementsByClassName('govuk-error-summary')[0]

    const errorH2Element = document.createElement('h2')
    errorH2Element.classList.add("govuk-error-summary__title")
    errorH2Element.innerHTML = messages("global.error.title")

    const errorElement = document.createElement('div')
    errorElement.setAttribute("role", "alert")
    errorElement.appendChild(errorH2Element)

    const errorBodyElement = document.createElement('div')
    errorBodyElement.classList.add("govuk-error-summary__body")

    const errorPrefixHiddenElement = document.createElement('span')
    errorPrefixHiddenElement.classList.add("govuk-visually-hidden")
    errorPrefixHiddenElement.innerHTML = messages("error.browser.heading.prefix")

    const errorSummaryList = document.createElement('ul')
    errorSummaryList.classList.add("govuk-list", "govuk-error-summary__list")

    const group = document.getElementsByClassName('govuk-form-group')[0]
    document.getElementsByClassName('govuk-form-group')[1].classList.remove("govuk-form-group--error")

    const fileUploadComponent = document.getElementById("file-upload-component");
    fileUploadComponent.removeAttribute("aria-describedby");
    const errorSection = document.getElementById('file-upload-component-error')
    const errorMessage = document.createElement("p")
    errorSection.appendChild(errorMessage)

    const file2Upload = document.getElementsByClassName('govuk-file-upload')[0]
    const maxFileSize = Number(file2Upload.dataset.maxFileSize)
    const fileExtensions = file2Upload.getAttribute("file-extensions").toLowerCase().split(',')

    const submitButton = document.querySelector('[name="submit"]')

    const regex = /^[0-9a-zA-Z][0-9a-zA-Z\.\-_ ]+$/

    let allowSubmit = true

    document.querySelector('form').onsubmit = (event) => {
        const file = file2Upload.files[0]

        if (!file2Upload.value) {
            event.preventDefault()
            showError(messages("fileUploadPage.selectFile"))
        }
        else if (!regex.test(file.name)) showError(messages("fileUploadPage.error.nameStart"))
        else if (file.size == 0) showError(messages("fileUploadPage.error.emptyFile"))
        else if (file.size > maxFileSize) showError(messages("fileUploadPage.error.fileSize", maxFileSize/1024/1024))
        else if (!hasExpectedExtension(file)) showError(messages("fileUploadPage.error.extension", fileExtensions.join(", ")))
        else if (allowSubmit) {
            allowSubmit = false
            return true
        }

        return false
    }

    function hasExpectedExtension(file) {
        const index = file.name.lastIndexOf('.')
        if (index == -1) return false
        const extension = file.name.toLowerCase().slice(index)
        return fileExtensions.includes(extension)
    }

    function resetError() {
        errorSummary.classList.add("govuk-visually-hidden")

        document.title = originalPageTitle

        while (errorSummaryList.firstChild) errorSummaryList.removeChild(errorSummaryList.firstChild)
        while (errorSummary.firstChild) errorSummary.removeChild(errorSummary.firstChild)

        errorMessage.innerHTML = ""
        group.classList.remove("govuk-form-group--error")
    }

    function showError(error) {
        resetError()
        errorSummary.appendChild(errorElement)
        errorSummary.appendChild(errorBodyElement)
        errorBodyElement.appendChild(errorSummaryList)

        fileUploadComponent.setAttribute("aria-describedby", "file-upload-component-error");
        showErrorSummary(error)

        document.title = messages("error.browser.heading.prefix") + " " + originalPageTitle

        group.classList.add("govuk-form-group--error")
        errorMessage.innerHTML = errorPrefixHiddenElement.outerHTML + error
    }

    function showErrorSummary(error) {
        const errorLink = document.createElement('a')
        errorLink.setAttribute('href', '#file-upload-component')

        const nodes = error.split('<br>')
        for (let ix = 0; ix < nodes.length; ) {
            errorLink.appendChild(document.createTextNode(nodes[ix]))
            if (++ix < nodes.length) errorLink.appendChild(document.createElement('br'))
        }

        const errorSummaryItem = document.createElement('li');
        errorSummaryItem.appendChild(errorLink)
        errorSummaryList.appendChild(errorSummaryItem)
        errorSummary.classList.remove("govuk-visually-hidden")

        errorLink.focus()
    }
})()
