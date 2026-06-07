import { $, $$ } from '#utils/dom.js'
import { request } from '#utils/request.js'
import { getFiles, getSelectedFiles } from '#modules/file.js'
import { showModal, hideModal } from '#components/modal.js'

let $modal = null

const buildModifyModalBody = () => {
  const $body = document.createElement('div')
  $body.id = 'file-modify-body'
  
  const bodyText = `
    <div class="search">
      <div class="find">
        <input placeholder = "请输入正则表达式" />
        <button type="button">Reduction</button>
      </div>
      <div class="replace">
        <input placeholder="请输入正则表达式" />
        <button type="button">Replace</button>
      </div>
    </div>
    <div class="replace-table">
      <table>
      <thead>
      <tr>
        <th>文件名称</th>
        <th>新文件名称</th>
      </tr>
      </thead>
      <tbody>
      </tbody>
      </table>
    </div>
  `
  $body.innerHTML = bodyText

  const {selectedFiles} = getSelectedFiles()
  $('.replace-table tbody', $body).innerHTML = selectedFiles.map(ff => {
    return `
      <tr data-id="${ff.id}">
        <td>${ff.name}</td>
        <td><div><textarea name="file-newname-${ff.id}">${ff.name}</textarea></div></td>
      </tr>
    `
  }).join('')

  $('.find > button', $body).addEventListener('click', () => {
    $$('.replace-table tbody > tr').forEach($item => {
      $('td:last-child textarea', $item).value = $('td:first-child').innerText
    })
  })
  $('.replace > button', $body).addEventListener('click', () => {
    const findText = $('.find input').value, replaceText = $('.replace input').value
    if(!findText || !replaceText) return
    let reg = new RegExp(findText)
    $$('.replace-table tbody > tr').forEach($item => {
      $('td:last-child textarea', $item).value = $('td:first-child').innerText.replace(reg, replaceText)
    })
  })

  return $body
}

const buildModifyModalFooter = ($modalBody) => {
  const $foot = document.createElement('div')
  $foot.id = 'file-modify-foot'
  const footText = `
    <button type="button" class="close">Close</button>
    <button type="button" class="ok">Ok</button>
  `
  $foot.innerHTML = footText

  $('button.close', $foot).addEventListener('click', () => {
    hideModal($modals.pop())
  })
  $('button.ok', $foot).addEventListener('click', async () => {
    const list = []
    $$('.replace-table tbody tr', $modalBody).forEach($item => {
      const oldName = $('td:first-child', $item).innerText
      const newName = $('textarea', $item).value.trim()
      if(!newName || oldName === newName) return
      const currentFiles = getFiles()
      const file = currentFiles.filter(a => a.id === +$item.dataset.id)[0]
      if(!!file) {
        list.push({
          id: file.id, fileFlag: file.fileFlag, newName
        })
      }
    })
    if(list.length > 0) {
      await request('/ftp/rename', {
        method: "POST",
        body: list
      })
      const {selectedFiles} = getSelectedFiles()
      for(let item of list) {
        selectedFiles.filter(ff => item.id === ff.id)[0].name = item.newName
        $(`.file-item[data-file-id="${item.id}"] .file-name`).innerText = item.newName
      }
      const { $selectedItems } = getSelectedFiles()
      $selectedItems.forEach($item => $item.classList.remove('selected'))
    }
    hideModal($modal)
  })
  return $foot
}

export const appendModifyModal = () => {
  $modal = showModal('批量修改名称', buildModifyModalBody(), buildModifyModalFooter())
}
