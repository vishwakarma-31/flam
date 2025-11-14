// Frame Statistics Interface
interface FrameStats {
    fps: number;
    width: number;
    height: number;
    processingTime: number;
    frameCount: number;
}

// WebSocket Message Interface
interface WebSocketMessage {
    type: 'frame' | 'stats';
    data?: string; // base64 image
    stats?: FrameStats;
}

class EdgeDetectionViewer {
    private canvas: HTMLCanvasElement;
    private ctx: CanvasRenderingContext2D;
    private placeholder: HTMLElement;
    
    // WebSocket connection
    private ws: WebSocket | null = null;
    private wsUrl: string = 'ws://localhost:8765';
    
    // UI Elements
    private connectBtn: HTMLButtonElement;
    private disconnectBtn: HTMLButtonElement;
    private fileInput: HTMLInputElement;
    private statusIndicator: HTMLElement;
    private statusText: HTMLElement;
    private serverInput: HTMLInputElement;
    private updateServerBtn: HTMLButtonElement;
    
    // Stats Elements
    private fpsElement: HTMLElement;
    private resolutionElement: HTMLElement;
    private processingTimeElement: HTMLElement;
    private frameCountElement: HTMLElement;
    private connectionTypeElement: HTMLElement;
    
    // Log Container
    private logContainer: HTMLElement;
    
    // Frame tracking
    private frameCount: number = 0;
    private lastFrameTime: number = 0;
    private fps: number = 0;
    
    constructor() {
        // Get canvas and context
        this.canvas = document.getElementById('frameCanvas') as HTMLCanvasElement;
        this.ctx = this.canvas.getContext('2d')!;
        this.placeholder = document.getElementById('placeholder')!;
        
        // Get UI elements
        this.connectBtn = document.getElementById('connectBtn') as HTMLButtonElement;
        this.disconnectBtn = document.getElementById('disconnectBtn') as HTMLButtonElement;
        this.fileInput = document.getElementById('fileInput') as HTMLInputElement;
        this.statusIndicator = document.getElementById('statusIndicator')!;
        this.statusText = document.getElementById('statusText')!;
        this.serverInput = document.getElementById('serverInput') as HTMLInputElement;
        this.updateServerBtn = document.getElementById('updateServer') as HTMLButtonElement;
        
        // Get stats elements
        this.fpsElement = document.getElementById('fps')!;
        this.resolutionElement = document.getElementById('resolution')!;
        this.processingTimeElement = document.getElementById('processingTime')!;
        this.frameCountElement = document.getElementById('frameCount')!;
        this.connectionTypeElement = document.getElementById('connectionType')!;
        
        // Get log container
        this.logContainer = document.getElementById('logContainer')!;
        
        this.initializeEventListeners();
        this.log('Viewer initialized', 'success');
    }
    
    private initializeEventListeners(): void {
        // Connect button
        this.connectBtn.addEventListener('click', () => this.connectToAndroid());
        
        // Disconnect button
        this.disconnectBtn.addEventListener('click', () => this.disconnect());
        
        // File input for static images
        this.fileInput.addEventListener('change', (e) => this.handleFileUpload(e));
        
        // Update server address
        this.updateServerBtn.addEventListener('click', () => this.updateServerAddress());
    }
    
    private connectToAndroid(): void {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.log('Already connected', 'error');
            return;
        }
        
        this.log(`Connecting to ${this.wsUrl}...`);
        
        try {
            this.ws = new WebSocket(this.wsUrl);
            
            this.ws.onopen = () => this.onWebSocketOpen();
            this.ws.onmessage = (event) => this.onWebSocketMessage(event);
            this.ws.onerror = (error) => this.onWebSocketError(error);
            this.ws.onclose = () => this.onWebSocketClose();
            
        } catch (error) {
            this.log(`Connection failed: ${error}`, 'error');
            this.updateConnectionStatus(false);
        }
    }
    
    private onWebSocketOpen(): void {
        this.log('Connected to Android app!', 'success');
        this.updateConnectionStatus(true);
        this.connectBtn.disabled = true;
        this.disconnectBtn.disabled = false;
        this.connectionTypeElement.textContent = 'WebSocket (Live)';
    }
    
    private onWebSocketMessage(event: MessageEvent): void {
        try {
            const message: WebSocketMessage = JSON.parse(event.data);
            
            if (message.type === 'frame' && message.data) {
                this.displayFrame(message.data);
                this.updateFPS();
            }
            
            if (message.type === 'stats' && message.stats) {
                this.updateStats(message.stats);
            }
            
        } catch (error) {
            // If it's a direct base64 string (not JSON)
            if (typeof event.data === 'string') {
                this.displayFrame(event.data);
                this.updateFPS();
            }
        }
    }
    
    private onWebSocketError(error: Event): void {
        this.log('WebSocket error occurred', 'error');
        console.error('WebSocket error:', error);
    }
    
    private onWebSocketClose(): void {
        this.log('Disconnected from Android app');
        this.updateConnectionStatus(false);
        this.connectBtn.disabled = false;
        this.disconnectBtn.disabled = true;
        this.ws = null;
    }
    
    private disconnect(): void {
        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
        this.updateConnectionStatus(false);
        this.connectBtn.disabled = false;
        this.disconnectBtn.disabled = true;
        this.log('Manually disconnected');
    }
    
    private handleFileUpload(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input.files?.[0];
        
        if (!file) return;
        
        this.log(`Loading file: ${file.name}`);
        
        const reader = new FileReader();
        reader.onload = (e) => {
            const base64 = e.target?.result as string;
            this.displayFrame(base64);
            this.connectionTypeElement.textContent = 'Static Image';
            this.log('Static image loaded successfully', 'success');
        };
        reader.readAsDataURL(file);
    }
    
    private displayFrame(imageData: string): void {
        const img = new Image();
        
        img.onload = () => {
            // Hide placeholder
            this.placeholder.style.display = 'none';
            
            // Update canvas size to match image
            this.canvas.width = img.width;
            this.canvas.height = img.height;
            
            // Draw image
            this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
            this.ctx.drawImage(img, 0, 0);
            
            // Update stats
            this.frameCount++;
            this.frameCountElement.textContent = this.frameCount.toString();
            this.resolutionElement.textContent = `${img.width} x ${img.height}`;
        };
        
        // Handle base64 with or without data URI prefix
        if (imageData.startsWith('data:')) {
            img.src = imageData;
        } else {
            img.src = `data:image/jpeg;base64,${imageData}`;
        }
    }
    
    private updateFPS(): void {
        const now = performance.now();
        
        if (this.lastFrameTime > 0) {
            const delta = now - this.lastFrameTime;
            this.fps = Math.round(1000 / delta);
            this.fpsElement.textContent = this.fps.toString();
        }
        
        this.lastFrameTime = now;
    }
    
    private updateStats(stats: FrameStats): void {
        if (stats.fps !== undefined) {
            this.fpsElement.textContent = stats.fps.toString();
        }
        if (stats.width !== undefined && stats.height !== undefined) {
            this.resolutionElement.textContent = `${stats.width} x ${stats.height}`;
        }
        if (stats.processingTime !== undefined) {
            this.processingTimeElement.textContent = `${stats.processingTime.toFixed(2)} ms`;
        }
        if (stats.frameCount !== undefined) {
            this.frameCount = stats.frameCount;
            this.frameCountElement.textContent = stats.frameCount.toString();
        }
    }
    
    private updateConnectionStatus(connected: boolean): void {
        if (connected) {
            this.statusIndicator.classList.remove('disconnected');
            this.statusIndicator.classList.add('connected');
            this.statusText.textContent = 'Connected';
        } else {
            this.statusIndicator.classList.remove('connected');
            this.statusIndicator.classList.add('disconnected');
            this.statusText.textContent = 'Disconnected';
            this.connectionTypeElement.textContent = 'None';
        }
    }
    
    private updateServerAddress(): void {
        const newUrl = this.serverInput.value.trim();
        
        if (newUrl && (newUrl.startsWith('ws://') || newUrl.startsWith('wss://'))) {
            this.wsUrl = newUrl;
            document.getElementById('serverAddress')!.textContent = newUrl;
            this.log(`Server address updated to: ${newUrl}`, 'success');
        } else {
            this.log('Invalid WebSocket URL. Must start with ws:// or wss://', 'error');
        }
    }
    
    private log(message: string, type: 'success' | 'error' | 'info' = 'info'): void {
        const timestamp = new Date().toLocaleTimeString();
        const logEntry = document.createElement('div');
        logEntry.className = `log-entry ${type}`;
        logEntry.innerHTML = `
            <span class="log-timestamp">[${timestamp}]</span>
            <span class="log-message">${message}</span>
        `;
        
        this.logContainer.appendChild(logEntry);
        this.logContainer.scrollTop = this.logContainer.scrollHeight;
        
        // Keep only last 50 entries
        while (this.logContainer.children.length > 50) {
            this.logContainer.removeChild(this.logContainer.firstChild!);
        }
    }
}

// Initialize viewer when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new EdgeDetectionViewer();
}); 