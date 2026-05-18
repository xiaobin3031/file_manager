import { request, uploadFile, baseUrl } from '../util/request.js'
import { $, $$ } from '../util/dom.js'
import { showModal, hideModal } from '../components/modal.js'
import { is_enter } from '../util/key_event.js'

let currentFiles = []
let $modals = []

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
        const $modal = showModal('新建', buildAddModalBody(), null)
        $modals.push($modal)
    })
    $('#ops > span.move').addEventListener('click', async (e) => {
        e.stopPropagation()
        const {selectedFiles} = getSelectedFiles()
        if(!selectedFiles || selectedFiles.length === 0) return
        const $body = await buildMoveModalBody()
        const $modal = showModal('迁移', $body, null)
        $modals.push($modal)
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
      const $body = buildModifyModalBody(selectedFiles)
      const $foot = await buildModifyModalFooter($body)
      const $modal = showModal('批量修改名称', $body, $foot)
      $modals.push($modal)
    })
    $('#ops > span.sort').addEventListener('click', async (e) => {
      e.stopPropagation()
      await request('/ftp/sortFilesByNameAsc', {
        method: "POST"
      })
      loadDirs()
    })
})

const getSelectedFiles = () => {
    const $selectedItems = $$('#app div.file-item.selected')
    const fileIds = Array.from($selectedItems).map(item => +item.dataset.fileId)
    return {
        selectedFiles: currentFiles.filter(file => fileIds.includes(file.id)),
        "$selectedItems": $selectedItems
    }
}
  
function resize() {
    const $root = document.getElementById('root');
    const height = `${window.innerHeight - 10}px`
    const width = `${window.innerWidth - 10}px`
    $root.style.height = height
    $root.style.width = width
}

async function loadDirs() {
    let dirs = await request('/ftp/listDirs')
    refreshDirs(dirs)
}

const getExtName = (file) => {
    if(file.fileFlag) return file.name.split('.').pop()
    return ''
}

const isVideo = (file) => {
    return !!file.fileType && file.fileType.indexOf("video/") === 0;
}
const isImage = (file) => {
    return !!file.fileType && file.fileType.indexOf("image/") === 0;
}
const isPdf = (file) => {
    return !!file.fileType && file.fileType === 'application/pdf';
}

const changeAddType = (type) => {
    $('#file-add-body .tab-item.active')?.classList.remove('active')
    $(`#file-add-body .tab-item.tab-${type}`).classList.add('active')
}
const testDownloadUrl = (e) => {

}

const dblClickFile = async (file) => {
    if(file.fileFlag) {
        if(isVideo(file)) {
          // todo 视频预览
        }else if(isImage(file)) {
          // todo 图片预览
        }else if(isPdf(file)) {
          // todo pdf预览
        }
    }else {
      const res = await request(`/ftp/changeDir`, {
        method: "POST",
        body: {
          id: file.id
        }
      })
      let showDefault = true
      if(!!res.lastFile) {
          if(window.confirm('有观看历史，是否进入？')) {
            showDefault = false
            dblClickFile(res.lastFile)
          }
      }
      if(showDefault) {
          refreshDirs(res)
      }
    }
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

const buildFileDom = (file) => {
    const $file = document.createElement('div')
    $file.classList.add('file-item')
    $file.draggable = true
    $file.dataset.fileId = file.id
    $file.dataset.fileId = file.id
    $file.addEventListener('dragstart', (e) => {
        e.dataTransfer.setData('dragFileId', e.currentTarget.dataset.fileId)
    })
    $file.addEventListener('drop', async (e) => {
        const dragFileId = +e.dataTransfer.getData('dragFileId')
        const fileId = +e.currentTarget.dataset.fileId
        if(dragFileId == fileId) return
        let idx = currentFiles.findIndex(item => item.id === +dragFileId)
        if(idx === -1) return
        const [movedItem] = currentFiles.splice(idx, 1)
        currentFiles.splice(idx, 0, movedItem)

        const body = currentFiles.map((item, index) => ({
            id: item.id,
            fileFlag: arguments.fileFlag,
            sort: item.sort
          }))
        await request('/ftp/sortFiles', {
          method: "POST",
          body: body
        })
    })
    $file.addEventListener('dragover', (e) => {
        e.preventDefault()
    })

    const bodyText = `
        <div class="file-sample"></div>
        <div class="file-info">
            <div class="file-name">${file.name}</div>
        </div>
    `
    $file.innerHTML = bodyText

    const $sample = $('.file-sample', $file)
    $sample.addEventListener('click', (e) => {
        e.currentTarget.parentNode.classList.toggle('selected')
        changeCheckOpsStatus()
    })
    $sample.addEventListener('dblclick', (e) => {
        dblClickFile(file)
    })

    if(file.fileFlag) {
        $sample.style.backgroundImage = `url("${baseUrl}/file/sample/${file.id}.webp")`
    } 
    else $file.classList.add('dir')
    return $file
}

function refreshDirs(dirs) {
    const files = dirs.files || []
    currentFiles = files
    const $app = $('#app')
    while($app.firstChild) {
        $app.removeChild($app.firstChild)
    }

    for(let i=0;i<files.length;i++) {
        const file = files[i]
        $app.appendChild(buildFileDom(file))
    }

    const breadText = dirs.path.map(p => {
        return `<span class="path">${p}</span>`
    })
    $('#bread').innerHTML = `/${breadText.join('/')}`
    $$('span.path:not(:last-child)').forEach($path => {
        $path.addEventListener('click', async (e) => {
            const res = await request('/ftp/changeDir', {
                method: 'POST',
                body: {
                    dirName: e.currentTarget.textContent
                }
            })
            refreshDirs(res)
        })
    })
}

const buildAddModalBody = () => {
  const $body = document.createElement('div')
    $body.id = 'file-add-body'

    const bodyText = `
        <div>
            <label><input type="radio" name="addType" value="1" checked="true" />文件夹</label>
            <label><input type="radio" name="addType" value="2" />文件</label>
            <label><input type="radio" name="addType" value="3" />下载任务</label>
            <label><input type="radio" name="addType" value="4" />下载计划</label>
        </div>
        <div>
            <div class="tab-item tab-1 active">
                <input type="text" placeholder="请输入文件夹名称" />
            </div>
            <div class="tab-item tab-2">
                <div> <input type="file" multiple="multiple" /><button type="button">上传</button></div>
                <div class="file-add-info">
                    <span></span>
                    <span></span>
                    <span class="filename"></span>
                </div>
                <div class="file-add-progress">
                    <div class="progress-bar"></div>
                    <span class="progress-text">0</span>
                </div>
            </div>
            <div class="tab-item tab-3">
                <textarea placeholder="请输入下载链接"></textarea>
            </div>
            <div class="tab-item tab-4">
                <div class="download-url-input">
                    <input type="text" placeholder="请输入下载网址"/>
                    <button type="button">测试</button>
                </div>
                <div class="xpath-input">
                  <textarea rows=3 placeholder="请输入xpath表达式"></textarea>
                </div>
                <div class="magnet-list"></div>
            </div>
        </div>
    `

    $body.innerHTML = bodyText

    $('.tab-1 > input', $body).addEventListener('keyup', async (e) => {
      if(is_enter(e, {ctrl: true})) {
        let val = e.target.value
        if(!val || !val.trim()) {
          return
        }
        val = val.trim()
        const foldId = await request('/ftp/addFold', {
          method: "POST",
          body: {
            dirName: val
          }
        })
        if(!!foldId) {
          const fold = {id: foldId, fileFlag: false, name: val}
          const $dom = buildFileDom(fold)
          $('#app').prepend($dom)
          currentFiles.unshift(fold)
          e.target.value = ''
          hideModal($modals.pop())
        }
      }
    })

    Array.from($body.querySelectorAll('input[name="addType"]')).forEach((input) => {
        input.addEventListener('change', (e) => {
            const type = e.target.value
            $('#file-add-body .tab-item.active')?.classList.remove('active')
            $(`#file-add-body .tab-item.tab-${type}`).classList.add('active')
        })
    })

    $('.tab-2 input[type="file"]', $body).addEventListener('change', (e) => {
        const files = e.target.files
        const $filename = $('.file-add-info span.filename', $body)
        const $spans = $$('.file-add-info span', $body)
        if(files.length === 0) {
            $spans[0].innerText = ''
            $filename.innerText = ''
            return
        }
        $spans[0].innerText = `1 / ${files.length}`
        $filename.innerText = `${files[0].name}`
        $('.tab-2', $body).classList.add('wait')
    })

    // 上传
    $body.querySelector('.tab-2 button').addEventListener('click', async (e) => {
        const files = $body.querySelector('.tab-2 input[type="file"]').files
        if(files.length === 0) {
            alert('请选择文件')
            return
        }
        const $progressBar = $('.file-add-progress .progress-bar', $body)
        const $progressText = $('.file-add-progress .progress-text', $body)
        const $progress = $('.file-add-progress', $body)
        $progressBar.style.width = '0'
        const $tab2 = $('.tab-2', $body)
        const $filename = $('.file-add-info span.filename', $body)
        for(let i=0;i<files.length;i++) {
            const file = files[i]
            const $spans = $$('.file-add-info span', $body)
            $spans[0].innerText = `${i + 1} / ${files.length}`
            $filename.innerText = `${file.name}`
            $tab2.classList.replace('wait', 'uploading')
            await uploadFile(file, (percent) => {
                if(percent === -1) return
                let pp = Math.min(percent, 100).toFixed(0)
                $progressBar.style.width = `${pp}%`
                $progressText.innerText = `${pp}`
                if (+pp === 100) {
                    $tab2.classList.replace('uploading', 'success')
                }
            })
            await sleep()
            $tab2.classList.replace('success', 'wait')
            $progressBar.style.width = '0'
            $progressText.innerText = '0'
            await sleep()
        }

        loadDirs()
    })

    $('.tab-3 textarea', $body).addEventListener('keyup', async (e) => {
      if(is_enter(e, {ctrl: true})) {
        let val = e.target.value
        if(!val || !val.trim()) return
        val = val.trim()
        const file = await request('/ftp/addDownload', {
          method: "POST",
          body: {
            magnet: val
          }
        })
        currentFiles.push(file)
        e.target.value = ''
        $('#app').appendChild(buildFileDom(file))
      }
    })

    $('.tab-4 .download-url-input button', $body).addEventListener('click', async(e) => {
      const val = e.currentTarget.previousElementSibling.value
      const xpath_val = $('.tab-4 .xpath-input textarea').value
      if(!val.trim() || !xpath_val?.trim()) return
      val = val.trim()
      let html = await request('/ftp/loadHtmlText', {
        method: "GET",
        body: {
          url: val
        }
      })
      if (!!html) {
        const parser = new DOMParser()
        const doc = parser.parseFromString(html, 'text/html')
        const xpathResult = document.evaluate(xpath_val, doc, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null)
        let results = []
        for(let i=0;i<xpathResult.snapshotLength;i++) {
          result.push(xpathResult.snapshotItem(i).textContent.trim())
        }
        const $magnetList = $('.tab-4 .magnet-list', $body)
        let $dom = document.createElement('div')
        $dom.innerText(`total: ${results.length}`)
        $magnetList.appendChild($dom)
        for(let uu of results) {
          $dom = document.createElement('div')
          $dom.innerText = uu
          $magnetList.appendChild($dom)
        }
      }
    })
    
    return $body
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
    hideModal($modals.pop())
    Array.from($selectedItems).filter(item => +item.dataset.id !== +foldId).forEach(item => item.remove())
  })

  return $body
}

const buildModifyModalBody = (selectedFiles) => {
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
      for(let item of list) {
        currentFiles.filter(ff => item.id === ff.id)[0].name = item.newName
        $(`.file-item[data-file-id="${item.id}"] .file-name`).innerText = item.newName
      }
      const { $selectedItems } = getSelectedFiles()
      $selectedItems.forEach($item => $item.classList.remove('selected'))
    }
    hideModal($modals.pop())
  })
  return $foot
}
