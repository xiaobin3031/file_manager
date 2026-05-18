import { $, $$ } from '../util/dom.js'
let zIndex = 5000

function buildModalOverlay() {
    const $overlay = document.createElement('div')
    $overlay.classList.add('modal-overlay')
    $overlay.style.zIndex = zIndex++
    return $overlay
}

function innerHideModal($modal) {
  if(!$modal) return

  document.body.removeChild($modal)
}

export function showModal(title, $body, $footer) {
    const $overlay = buildModalOverlay()
    const modalText = `
        <div class="modal">
            <div class="modal-header">
                <span class="modal-title">${title || ''}</span>
                <span class="modal-close">&times;</span>
            </div>
            <div class="modal-body">
            </div>
            <div class="modal-footer">
            </div>
        </div>
    `
    $overlay.innerHTML = modalText
    $('.modal-body', $overlay).appendChild($body)
    if($footer) $('.modal-footer', $overlay).appendChild($footer)
    document.body.appendChild($overlay)

    $('.modal-close', $overlay).addEventListener('click', () => {
      innerHideModal($overlay)
    })

    return $overlay
}

export const hideModal = innerHideModal
