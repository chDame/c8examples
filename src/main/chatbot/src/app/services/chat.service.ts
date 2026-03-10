import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from 'src/environments/environment';
import { SafeHtml } from '@angular/platform-browser';

export interface ChatMessage {
  thinking: boolean;
  jobKey?: number;
  message: string;
  contentHtml?: SafeHtml;
  user?: boolean;
  timestamp?: Date;
}

export interface ChatHistoryResponse {
  messages: ChatMessage[];
  lastJobKey?: number;
}

export interface ChatSession {
  sessionId: string;
  title?: string;
}


@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private eventSource?: EventSource;
  private messageSubject = new Subject<ChatMessage>();

  constructor(
    private http: HttpClient,
    private authService: AuthService,
    private ngZone: NgZone
  ) {}

  public buildFormData(variables: any, files?: File[]): FormData {
    const formData = new FormData();
    if (files != null && files.length>0) {
      files.forEach((file: any) => {
        formData.append("documents", file, file.name);
      });
    }
    formData.append('body', new Blob([JSON.stringify(variables)], {
      type: "application/json"
    }));

    return formData;
  }

  startNewChat(message: string, files?: File[]): Observable<any> {
    const body = {
      message: message,
      author: this.authService.getCurrentUser()
    };

    return this.http.post<any>(`${environment.backend}/api/chatbot`, this.buildFormData(body, files));
  }

  sendMessage(sessionId: string, message: string, jobKey: number, files?: File[]): Observable<any> {

    const body = {
      message: message,
      jobKey: jobKey,
      author: this.authService.getCurrentUser()
    };

    return this.http.post<any>(`${environment.backend}/api/chatbot/userInput/${sessionId}`, this.buildFormData(body, files));
  }

  connectToSSE(sessionId: string): Observable<any> {
    if (this.eventSource) {
      this.eventSource.close();
    }

    this.eventSource = new EventSource(`${environment.backend}/api/chatbot/chat-sse/${sessionId}`);
    
    this.eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        this.ngZone.run(() => {
          this.messageSubject.next(data);
        });
      } catch (error) {
        console.error('Error parsing SSE message:', error);
      }
    };

    this.eventSource.onerror = (error) => {
      console.error('SSE connection error:', error);
    };
    return this.messageSubject.asObservable();
  }

  disconnectSSE(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = undefined;
    }
  }

  getChatSessions(): Observable<any> {
    return this.http.get<any>(`${environment.backend}/api/instances/chatbot/ACTIVE`);
  }

  loadChatHistory(sessionId: string): Observable<ChatHistoryResponse> {
    return this.http.get<ChatHistoryResponse>(`${environment.backend}/api/chatbot/history/${sessionId}`);
  }
}