import { request, uploadFile, baseUrl } from '#utils/request'
import { $, $$ } from '#utils/dom'
import { is_enter } from '#utils/key_event'
import { buildFileDom } from '#pages/fileDom'
import { appendAddModal } from '#components/addModal'
import { appendModifyModal } from '#pages/modifyModal'
import { refreshDirs, loadDirs, getFiles, getSelectedFiles } from '#pages/file'

window.addEventListener('resize', () => {
  resize()
})
document.addEventListener('DOMContentLoaded', () => {
  resize()

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

    if (window.confirm('是否删除这些文件/文件夹')) {
      await request('/ftp/unzipFile', {
        method: 'POST',
        body: selectedFiles.map(item => item.id)
      })
      $selectedItems.forEach(item => item.classList.remove('selected'))
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
})
  
function resize() {
    const $root = document.getElementById('root');
    const height = `${window.innerHeight - 10}px`
    const width = `${window.innerWidth - 10}px`
    $root.style.height = height
    $root.style.width = width
}
const testDownloadUrl = (e) => {

}

const changeCheckOpsStatus = () => {
    const selectedCount = $$('#app div.file-item.selected').length
    if(selectedCount === 0) {
        $('#ops > span.check').classList.remove('check-checked', 'check-part')   
    }else if(selectedCount === $$('#app div.file-item').length) {
        $('#ops > span.check').classList.add('check-checked')
        $('#ops > span.check').classList.remove('check-part')
    }else if(selectedCount > 0) {
        $('#ops > span.check').classList.remove('check-checked')
        $('#ops > span.check').classList.add('check-part')
    }
}

