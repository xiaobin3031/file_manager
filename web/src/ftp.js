import './ftp.css'
import { request, downloadFile } from '#utils/request.js'
import { $, $$ } from '#utils/dom.js'
import { is_enter } from '#utils/key_event.js'
import { buildFileDom } from '#modules/fileDom.js'
import { appendAddModal } from '#modules/addModal.js'
import { appendModifyModal } from '#modules/modifyModal.js'
import { appendMoveModal } from '#modules/moveModal.js'
import { refreshDirs, loadDirs, getFiles, getSelectedFiles, changeCheckOpsStatus } from '#modules/file.js'

document.addEventListener('DOMContentLoaded', () => {
  loadDirs()

  $('#ops > span.back').addEventListener('click', async (e) => {
    e.stopPropagation()
    let res = await request('/ftp/goBack')
    refreshDirs(res)
  })
  $('#ops > span.refresh').addEventListener('click', (e) => {
    e.stopPropagation()
    loadDirs()
  })
  $('#ops > span.add').addEventListener('click', (e) => {
    e.stopPropagation()
    appendAddModal();
  })
  $('#ops > span.move').addEventListener('click', async (e) => {
    e.stopPropagation()
    const {selectedFiles} = getSelectedFiles()
    if(!selectedFiles || selectedFiles.length === 0) return
    appendMoveModal()
  })
  $('#ops > span.check').addEventListener('click', (e) => { 
    e.stopPropagation()
    const classList = e.currentTarget.classList
    if(classList.contains('check-checked')) {
        $$('#app div.file-item.selected').forEach(item => item.classList.remove('selected'))
        classList.remove('check-checked')
    }else if(classList.contains('check-part')) {
        $$('#app div.file-item:not(.selected)').forEach(item => item.classList.add('selected'))
        classList.remove('check-part')
        classList.add('check-checked')
    }else{
        $$('#app div.file-item').forEach(item => item.classList.add('selected'))
        classList.add('check-checked')
    }
  })
  $('#ops > span.delete').addEventListener('click', async (e) => {
    e.stopPropagation()
    // 删除选中的文件或者目录
    const { selectedFiles, $selectedItems } = getSelectedFiles()
    if(selectedFiles.length === 0) {
      alert('请选择要删除的文件或目录')
      return
    }
    if(confirm('确定要删除选中的文件或目录吗？')) {
      // 执行删除操作
      await request('/ftp/removeFile', {
        method: 'POST',
        body: {
            files: selectedFiles
        }
      })
      const $parent = $selectedItems[0].parentNode
      $selectedItems.forEach(item => $parent.removeChild(item))
      changeCheckOpsStatus()
    }
  })
  $('#ops > span.unzip').addEventListener('click', async (e) => {
    e.stopPropagation()
    const { selectedFiles, $selectedItems } = getSelectedFiles()
    if(selectedFiles.length === 0) return

    if (window.confirm('是否解压这些文件/文件夹')) {
      await request('/ftp/unzipFile', {
        method: 'POST',
        body: selectedFiles.map(item => item.id)
      })
      refreshDirs()
    } 
  })
  $('#ops > span.modify').addEventListener('click', async (e) => {
    e.stopPropagation()
    const { selectedFiles } = getSelectedFiles()
    if(!selectedFiles || selectedFiles.length === 0) return
    appendModifyModal()
  })
  $('#ops > span.sort').addEventListener('click', async (e) => {
    e.stopPropagation()
    await request('/ftp/sortFilesByNameAsc', {
      method: "POST"
    })
    loadDirs()
  })
  $('#ops > span.download').addEventListener('click', (e) => {
    const {selectedFiles} = getSelectedFiles()
    if(selectedFiles.length != 1) {
      window.alert("仅支持单个文件的下载")
      return
    }
    downloadFile(selectedFiles[0].id)
  })
  $('#ops > span.comic').addEventListener('click', async (e) => {
    const {selectedFiles} = getSelectedFiles()
    if(selectedFiles.length == 0) {
      window.alert("请选择要生成cbz的文件/文件夹")
      return
    }
    await request('/ftp/createCbz', {
      method: "POST",
      body: selectedFiles.map(item => item.id)
    })
    loadDirs()
  })
})

const testDownloadUrl = (e) => {

}

