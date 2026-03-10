import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ChatService, ChatMessage, ChatHistoryResponse } from '../../services/chat.service';
import { Subscription } from 'rxjs';

import { DomSanitizer, SafeHtml } from "@angular/platform-browser";

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./chat.component.html",
  styleUrl: "./chat.component.css"
})
export class ChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messagesContainer') messagesContainer!: ElementRef;
  
  messages: ChatMessage[] = [];
  userInput = '';
  selectedFiles: File[] = [];
  isLoading = false;
  sidebarCollapsed = false;
  currentSessionId: string | null = null;
  lastJobKey: number | null = null;
  chatSessions: any[] = [];
  
  private messageSubscription?: Subscription;
  private shouldScrollToBottom = false;

  constructor(
    private authService: AuthService,
    private chatService: ChatService,
    private router: Router,
    private sanitizer: DomSanitizer
  ) {
    // Collapse sidebar on small screens by default
    this.sidebarCollapsed = window.innerWidth < 900;
  }

  ngOnInit(): void {
    this.loadChatSessions();
  }

  ngOnDestroy(): void {
    this.messageSubscription?.unsubscribe();
    this.chatService.disconnectSSE();
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollToBottom) {
      this.scrollToBottom();
      this.shouldScrollToBottom = false;
    }
  }

  loadChatSessions(): void {
    this.chatService.getChatSessions().subscribe({
      next: (response) => {
        this.chatSessions = response.items;
        for (const instance of this.chatSessions) {
          const variables = response.variables["" + instance.processInstanceKey];
          instance.variables = variables;
        }
      },
      error: (error) => {
        console.error('Error loading chat sessions:', error);
      }
    });
  }

  startNewChat(): void {
    this.currentSessionId = null;
    this.messages = [];
    this.lastJobKey = null;
    if (this.messageSubscription) {
      this.messageSubscription?.unsubscribe();
    }
    this.chatService.disconnectSSE();
  }

  loadChatSession(sessionId: string): void {
    this.currentSessionId = sessionId;
    this.lastJobKey = null;
    this.messages = [];
    if (this.messageSubscription) {
      this.messageSubscription.unsubscribe();
    }
    this.chatService.disconnectSSE();

    this.chatService.loadChatHistory(sessionId).subscribe({
      next: (response: ChatHistoryResponse) => {
        this.messages = (response.messages || []).map(m => this.processHistoryMessage(m));
        this.lastJobKey = response.lastJobKey || null;
        this.shouldScrollToBottom = true;
      },
      error: (error) => {
        console.error('Error loading chat history:', error);
        this.messages = [];
      }
    });

    const messageObservable = this.chatService.connectToSSE(sessionId);
    this.messageSubscription = messageObservable.subscribe(message => {
      this.handleIncomingMessage(message);
    });
  }

  private processHistoryMessage(message: ChatMessage): ChatMessage {
    if (!message.user && message.message) {
      const processed = message.message
        .replaceAll(/([\\*]{2})(((?![\\*]{2}).)*)([\\*]{2})/gi, '<b>$2</b>')
        .replaceAll('\n', '<br/>');
      message.contentHtml = this.sanitizer.bypassSecurityTrustHtml(processed);
    }
    return message;
  }

  sendMessage(): void {
    if (!this.userInput.trim() || this.isLoading) return;

    const messageText = this.userInput.trim();
    
    // Add user message to chat
    this.messages.push({
      message: messageText,
      contentHtml: undefined,
      user: true,
      thinking: false,
      timestamp: new Date()
    });

    this.userInput = '';
    this.selectedFiles = [];
    this.isLoading = true;
    this.shouldScrollToBottom = true;

    if (!this.currentSessionId) {
      // Start new chat
      this.chatService.startNewChat(messageText, this.selectedFiles).subscribe({
        next: (response) => {
          this.currentSessionId = response.sessionId;
          const messageObservable = this.chatService.connectToSSE(response.sessionId);
          
    this.messageSubscription = messageObservable.subscribe(message => {
      this.handleIncomingMessage(message);
    });
          this.isLoading = false;
          this.loadChatSessions(); // Refresh sessions list
        },
        error: (error) => {
          console.error('Error starting new chat:', error);
          this.isLoading = false;
        }
      });
    } else {
      // Continue existing chat
      if (this.lastJobKey) {
        this.chatService.sendMessage(this.currentSessionId, messageText, this.lastJobKey, this.selectedFiles).subscribe({
          next: () => {
            this.isLoading = false;
          },
          error: (error) => {
            console.error('Error sending message:', error);
            this.isLoading = false;
          }
        });
      } else {
        this.isLoading = false;
      }
    }
  }

  handleIncomingMessage(message: ChatMessage): void {
    // Remove any existing thinking messages
    this.messages = this.messages.filter(msg => !msg.thinking);
    message.message = message.message.replaceAll(/([\\*]{2})(((?![\\*]{2}).)*)([\\*]{2})/gi, "<b>$2</b>").replaceAll("\n", "<br/>");
    message.contentHtml = this.sanitizer.bypassSecurityTrustHtml(message.message);
    // Add new message
    this.messages.push(message);

    if (!message.thinking && message.jobKey) {
      this.lastJobKey = message.jobKey;
    }

    this.shouldScrollToBottom = true;
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    if (files) {
      this.selectedFiles = Array.from(files);
    }
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }
}