(() => {
    const messages = (key, interpolation) => {
        const enMessages = {
            "uploadFileComponent.error.nameStart": "File name must start with a letter or number, and only contain hyphen, underscore or dot as special characters",
            "uploadFileComponent.error.fileSize" : `File size must not be bigger than ${interpolation} Megabytes (MB)`,
            "uploadFileComponent.error.extension" : `File must have an extension of ${interpolation}`,
            "uploadFileComponent.selectFile" : "Select a file",
            "uploadFileComponent.error.emptyFile": "The selected file is empty"
        }
        const cyMessages = {
            "uploadFileComponent.error.nameStart": "Mae’n rhaid i enw’r ffeil ddechrau gyda llythyren neu rif, a chynnwys cysylltnod, tanlinell, neu ddot yn unig fel cymeriadau arbennig",
            "uploadFileComponent.error.fileSize" : `Mae’n rhaid i faint y ffeil beidio â bod yn fwy na ${interpolation} Megabeit (MB)`,
            "uploadFileComponent.error.extension" : `Mae’n rhaid i estyniad y ffeil fod yn ${interpolation}`,
            "uploadFileComponent.selectFile" : "Dewiswch ffeil",
            "uploadFileComponent.error.emptyFile": "Mae'r ffeil a ddewiswyd yn wag"
        }
        const lang = document.documentElement.getAttribute("lang")
        if (lang === "cy") return cyMessages[key]
        else return enMessages[key]
    }
    const errorSummary = document.getElementsByClassName('govuk-error-summary')[0]
    const errorSummaryList = document.getElementsByClassName('govuk-error-summary__list')[0]

    const group = document.getElementsByClassName('govuk-form-group')[0]
    document.getElementsByClassName('govuk-form-group')[1].classList.remove("govuk-form-group--error")

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
        if (!regex.test(file.name)) showError(messages("uploadFileComponent.error.nameStart"))
        else if (file.size == 0) showError(messages("uploadFileComponent.error.emptyFile"))
        else if (file.size > maxFileSize) showError(messages("uploadFileComponent.error.fileSize", maxFileSize/1024/1024))
        else if (!hasExpectedExtension(file)) showError(messages("uploadFileComponent.error.extension", fileExtensions.join(", ")))
    }

    let allowSubmit = true

    document.querySelector('form').onsubmit = (event) => {
        if (!file2Upload.value) {
            event.preventDefault()
            showError(messages("uploadFileComponent.selectFile"))
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
