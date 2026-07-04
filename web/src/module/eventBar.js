// 底部事件栏
import { uploadFile } from '#utils/request.js'
import { $, $$ } from '#utils/dom.js'

class EventBar {
  constructor() {
    this.$dom = $('footer > div.event-bar')
    this.queue = []
    this.current = null
    this.total = 0
    this.finished = 0
    this.onFinish = []
  }

  add(event) {
    this.total++
    event.attach(this)
    this.queue.push(event)
    this.next()
  }

  addOnFinish(fn) {
    if(this.onFinish.indexOf(fn) === -1) this.onFinish.push(fn)
  }

  async next() {
    if(this.current) return
    const event = this.queue.shift()

    if(!event) return

    this.current = event
    this.refresh()

    try {
      await event.execute()
      this.finished++
      if(this.finished === this.total) {
        if(this.onFinish.length > 0) {
          for(let fn of this.onFinish) {
            await fn()
          }
          this.onFinish = []
        }
        this.finished = 0
        this.total = 0
      }
    }finally{
      this.current = null
      this.refresh()
      this.next()
    }
  }
  
  refresh() {
    if(!this.current) {
      this.$dom.innerHTML = ''
      return
    }

    this.$dom.innerHTML = `
      <span>${this.finished} / ${this.total}</span>
      <span>${this.current.title}</span>
      <progress value="${this.current.progress}" max="100"></progress>
    `
  }
}

class BaseEvent {

  constructor(title) {
    this.title = title
    this.progress = 0
    this.status = 'waiting'
    this.bar = null
  }

  attach(bar) {
    this.bar = bar
  }

  setProgress(progress) {
    this.progress = progress
    this.refresh()
  }

  setTitle(title) {
    this.title = title
    this.refresh()
  }

  refresh() {
    this.bar?.refresh()
  }

  async execute() {}
}

export const eventBar = new EventBar()

export class UploadEvent extends BaseEvent {
  
  constructor(file, foldId) {
    super("上传: " + file.name)
    this.file = file
    this.foldId = foldId
  }

  async execute() {
    await uploadFile({file: this.file, foldId: this.foldId}, (percent) => {
      this.setProgress(Math.min(percent, 100).toFixed(0))
    })
  }
}
