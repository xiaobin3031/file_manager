import { $, $$ } from '#utils/dom.js'
import { request } from '#utils/request.js'
import { showModal, hideModal } from '#components/modal.js'
import { getSelectedFiles } from '#modules/file.js'

let $modal = null

export const appendMoveModal = async () => {
  const $body = await buildMoveModalBody()
  $modal = showModal('迁移', $body, null)
}

const buildMoveModalBody = async () => {
  const $body = document.createElement('div')
  $body.id = 'file-move-body'
  const childrenFolds = await request('/ftp/foldByParentId', {
    method: "GET",
    body: {
      parentId: 0
    }
  })
  const bodyText = `
    <div class="fold-items">
      ${childrenFolds.map(item => {
        return `<div data-id="${item.id}">${item.name}</div>`
      }).join('')}
    </div>
  `
  $body.innerHTML = bodyText

  $body.addEventListener('click', async(e) => {
    const $el = e.target
    if($el.classList.contains('active')) return
    if($el.parentNode.classList.contains('fold-items')) {
      const foldId = $el.dataset.id
      $('div.active', $el.parentNode)?.classList.remove('active')
      $el.classList.add('active')
      const folds = await request('/ftp/foldByParentId', {
        method: "GET",
        body: {
          parentId: foldId
        }
      })
      while($el.parentNode.nextElementSibling) {
        $el.parentNode.nextElementSibling.remove()
      }
      if(folds && folds.length > 0) {
        const $foldItems = document.createElement('div')
        $foldItems.classList.add('fold-items')
        $foldItems.innerHTML = folds.map(item => {
          return `<div data-id="${item.id}">${item.name}</div>`
        }).join('')
        $body.appendChild($foldItems)
      }
    }
  })
  
  $body.addEventListener('dblclick', async(e) => {
    let { selectedFiles, $selectedItems } = getSelectedFiles()
    if(!selectedFiles || selectedFiles.length == 0) return
    const $el = e.target
    const foldId = $el.dataset.id
    if (selectedFiles[0].foldId === +foldId) return
    selectedFiles = selectedFiles.filter(a => a.id !== +foldId)
    if(selectedFiles.length === 0) return
    await request('/ftp/moveFile', {
      method: "POST",
      body: {
        files: selectedFiles,
        foldId: foldId
      }
    })
    hideModal($modal)
    Array.from($selectedItems).filter(item => +item.dataset.id !== +foldId).forEach(item => item.remove())
  })

  return $body
}
