(() => {
    const messages = (key, interpolation) => {
        const message = window.messages[key]
        return message.replace("{0}", interpolation)
    }
    const errorSummary = document.getElementsByClassName('govuk-error-summary')[0]
    const errorSummaryBody = document.getElementsByClassName('govuk-error-summary__body')[0]
    const errorSummaryList = document.createElement('ul')
    errorSummaryList.classList.add("govuk-list", "govuk-error-summary__list")
    errorSummaryBody.appendChild(errorSummaryList)

    const group = document.getElementsByClassName('govuk-form-group')[0]
    document.getElementsByClassName('govuk-form-group')[1].classList.remove("govuk-form-group--error")

    const fileUploadComponent = document.getElementById("file-upload-component");
    fileUploadComponent.removeAttribute("aria-describedby");
    const errorSection = document.getElementById('file-upload-component-error')
    const errorMessage = document.createElement("span")
    errorSection.appendChild(errorMessage)

    const file2Upload = document.getElementsByClassName('govuk-file-upload')[0]
    const maxFileSize = Number(file2Upload.dataset.maxFileSize)
    const fileExtensions = file2Upload.getAttribute("file-extensions").toLowerCase().split(',')

    const submitButton = document.querySelector('[name="submit"]')

    const regex = /^[0-9a-zA-Z][0-9a-zA-Z\.\-_ ]+$/

    file2Upload.onchange = function() {
        resetError()
        const file = this.files[0]
        if (!regex.test(file.name)) showError(messages("fileUploadPage.error.nameStart"))
        else if (file.size == 0) showError(messages("fileUploadPage.error.emptyFile"))
        else if (file.size > maxFileSize) showError(messages("fileUploadPage.error.fileSize", maxFileSize/1024/1024))
        else if (!hasExpectedExtension(file)) showError(messages("fileUploadPage.error.extension", fileExtensions.join(", ")))
    }

    let allowSubmit = true

    document.querySelector('form').onsubmit = (event) => {
        if (!file2Upload.value) {
            event.preventDefault()
            showError(messages("fileUploadPage.selectFile"))
        }
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
        while (errorSummaryList.firstChild) errorSummaryList.removeChild(errorSummaryList.firstChild)

        errorMessage.innerHTML = ""
        group.classList.remove("govuk-form-group--error")

        submitButton.disabled = false
    }

    function showError(error) {
        fileUploadComponent.setAttribute("aria-describedby", "file-upload-component-error");
        showErrorSummary(error)

        group.classList.add("govuk-form-group--error")
        errorMessage.innerHTML = error

        submitButton.disabled = true
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
    }
})()
