import { request, uploadFile, baseUrl } from './util/request.js'
import { $, $$ } from './util/dom.js'
import { showModal, hideModal } from './util/modal.js'
import { is_enter } from './util/key_event.js'

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
    $('#ops > span.move').addEventListener('click', (e) => {
        e.stopPropagation()
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
    $('#ops > span.modify').addEventListener('click', (e) => {
        e.stopPropagation()
    })
    $('#ops > span.sort').addEventListener('click', (e) => {
        e.stopPropagation()
    })
    $('#ops > span.mirgrate').addEventListener('click', (e) => {
        e.stopPropagation()
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

const sleep = (second = 1) => new Promise(resolve => setTimeout(resolve, second * 1000))
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
