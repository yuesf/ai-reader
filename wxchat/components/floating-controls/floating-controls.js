// floating-controls.js
Component({
  properties: {
    // 控制按钮位置：right-center, right-top, right-bottom
    position: {
      type: String,
      value: 'right-center'
    },
    // 当前缩放比例
    scale: {
      type: Number,
      value: 1.0
    }
  },

  data: {
    
  },

  methods: {
    /**
     * 放大按钮点击事件
     */
    onZoomIn() {
      console.log('FloatingControls: onZoomIn called');
      const newScale = Math.min(3.0, this.properties.scale + 0.25);
      this.triggerEvent('zoomIn', {
        scale: newScale
      });
    },

    /**
     * 缩小按钮点击事件
     */
    onZoomOut() {
      console.log('FloatingControls: onZoomOut called');
      const newScale = Math.max(0.5, this.properties.scale - 0.25);
      this.triggerEvent('zoomOut', {
        scale: newScale
      });
    },

    /**
     * 重置按钮点击事件
     */
    onReset() {
      console.log('FloatingControls: onReset called');
      this.triggerEvent('reset', {
        scale: 1.0
      });
    }
  }
});
