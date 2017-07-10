# Enhancing Breakpad
## Overview
As we mentioned in [README.md](README.md), capturing _only_ the crashed thread stack leaves a lot of essential data out of scope. If your library uses smart pointers or works on strings, the actual data are allocated on the (general or special purpose) heap, not on the stack.

What we'd want to dump then is a ["Caterpillar graph"](https://en.wikipedia.org/wiki/Caterpillar_tree) whose central path is the stack.

Obviously, not all addresses in the stack are valid pointers, so we'll have to check actual memory address readability. This is best done per memory page. We'll also reserve a certain area _before_ the pointer address (for class inheritance, vtables and stuff like) and a greater area _after_ the pointer address to capture the full contents of the object pointed to.

When the crashed stack is known (after the crash), we copy its contents out to a preallocated memory area, then sort it in place and round the pointers pagewise. Contiguous page ranges can be dumped as a single memory area.

Unfortunately, Breakpad public API only allows to register "memory areas of interest" in advance. Creating data structures and populating linked lists in a compromised context may cause further memory corruption, preventing the dump from reporting or rendering it inaccurate. It is, however, possible to register a sufficient number of zero-length chunks ahead of time and fill in the actual addresses at crash time.

## Costs

Let's say we traverse two full 4K pages of the crashed stack, and the pointer size is 4 bytes. If each pointer is valid and points to a unique page, 8Mb are dumped. In practice, due to redundancy, a typical dump size is 1.5..2Mb, GZipped to around 300..500Kb.

## Sample implementation

_Note: the below sample code snippets are shared for reference. You don't have to reproduce them literally, and a certain degree of understanding the way they work is encouraged._

    #include <stdio.h>
    #include <unistd.h>
    #include <sys/mman.h>
    #include <algorithm>
    
    #include "breakpadWrapper.h"
    #define private public
    #include "client/linux/handler/exception_handler.h"
    #include "client/linux/handler/minidump_descriptor.h"
    #include "client/linux/dump_writer_common/ucontext_reader.h"

    using namespace google_breakpad;
    
Yes, "define private public". We'll only use it to overwrite private `AppMemory` fields (`ptr` and `length`).

Let's define the page arithmetic now.
    
    namespace breakpad_user_integration {
        // these values are compilation time constants
        const size_t pt_size = sizeof(void*);
        const size_t STACK_PAGES = 2;
        const size_t BYTES_PRIOR = 256;
        
        // these statics are queried at run time but the queries are idempotent (the same results expected each time)
        static size_t page_size;
        static size_t page_half;
        static size_t mem_count;
       
        // this only needs to be used once
        static uintptr_t* sortedBuf = NULL;
    
        // this one is a dummy marker. whatever equals it may be discarded.
        static AppMemory dummyChunk;
        static int handler_count = 0;
        
        inline uintptr_t ToPage(uintptr_t address) {
            return address & (-page_size);
        }
        
        inline bool IsPageMapped(void* page_address) {
            unsigned char vector;
            return 0 /*OK*/ == mincore(page_address, page_size, &vector);
        }
        
        inline bool IsPageMapped(uintptr_t page_address) {
            return IsPageMapped(reinterpret_cast<void*>(page_address));
        }
        
        inline bool IsAddressMapped(uintptr_t address) {
            return IsPageMapped(ToPage(address));
        }

The only system function called here is `mincore` that checks if a specific page is available w/o swapping. Considering the unpopularity of virtual memory on Android devices, this is effectively a check that a specific address belongs to mapped memory at all.

Now we create a buffer for sorting a stack copy:

        void EnsureBuf() {
            if (sortedBuf == NULL) {
                size_t sizeOfBuf = page_size * STACK_PAGES;
                // could be malloc(sizeOfBuf) but I preferred a dedicated mapping
                sortedBuf = reinterpret_cast<uintptr_t*>(mmap(NULL, sizeOfBuf,
                    PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0));
                // assert sortedBuf != null
            }
        }

HeapContext is a custom "user context" class. Breakpad references its instances by an opaque `void*` pointer. Its entire state consists of a pointer to `AppMemoryList`, Breakpad's linked list of memory ranges of special interest. `Attach` is called at initialization time, `CollectData` at crash time.

        class HeapContext {
        private:
            AppMemoryList* mList;
    
        public:
            void Attach(ExceptionHandler * eh) {
                AppMemoryList& memoryList = eh->app_memory_list_;
                // preallocate mem_count dummy chunks
                for (int i = 0; i < mem_count; ++i) {
                    memoryList.push_back(dummyChunk);
                }
                mList = &memoryList;
            }
            
            void TryIncludePage(AppMemoryList::reverse_iterator& chunk,
                    bool allowMerge, uintptr_t& fault, uintptr_t page_address) {
                if (page_address != fault) {
                    // try merge
                    if (allowMerge) {
                        uintptr_t last_address = reinterpret_cast<uintptr_t>(chunk->ptr) + chunk->length;
                        if (page_address >= last_address) {
                            if (IsPageMapped(page_address)) {
                                if (page_address > last_address) {
                                    --chunk;
                                    chunk->ptr = reinterpret_cast<void*>(page_address);
                                    chunk->length = page_size;
                                } else {
                                    chunk->length += page_size;
                                }
                            } else {
                                fault = page_address;
                            }
                        }
                    } else {
                        // map, then create chunk
                        if (IsPageMapped(page_address)) {
                            --chunk;
                            chunk->ptr = reinterpret_cast<void*>(page_address);
                            chunk->length = page_size;
                        } else {
                            fault = page_address;
                        }
                    }
                }
            }
            
            void CollectData(uintptr_t stackTop, uintptr_t stackSize) {
                memcpy(sortedBuf, reinterpret_cast<void*>(stackTop), stackSize);
                uintptr_t  ptr_count = stackSize / pt_size;
                uintptr_t* sortedPtr = sortedBuf;
                uintptr_t* sortedEnd = sortedBuf + ptr_count;
                std::sort(sortedPtr, sortedEnd); // qsort
                
                AppMemoryList::reverse_iterator start = mList->rend();
                AppMemoryList::reverse_iterator chunk = start;
                uintptr_t fault  = 0xffffffff;
                while (sortedPtr != sortedEnd) {
                    uintptr_t stackAddr = *(sortedPtr++);
                    if (stackAddr >= BYTES_PRIOR) {
                        stackAddr -= BYTES_PRIOR;
                    }
                    uintptr_t stackPage = ToPage(stackAddr);
                    TryIncludePage(chunk, chunk != start, fault, stackPage);
                    if (stackAddr > stackPage + page_half) {
                        TryIncludePage(chunk, chunk != start, fault, stackPage + page_size);
                    }
                }
            }
        };
    }

Now the actual integration layer.

    extern "C" {
        using namespace breakpad_user_integration;

_Optional: we'd like to make sure PTRACE_GETFPREGS works properly, or else crash reporting in AArch32 compatibility mode (32-bit code, 64-bit CPU) is broken. Either PTRACE_GETVFPREGS [=27] or legacy PTRACE_GETFPREGS [=14] works; if neither works, fill in e.g. PTRACE_GETREGS [=12] and read the general purpose registers instead to at least let the dump writer continue. This workaround is, however, out of scope of this tutorial; also, you may be using a Breakpad version that has the issue fixed._

        bool VFPRCallback(const void* crash_context, size_t crash_context_size, void* context) {
            user_fpregs canary;
            /// try calling sys_ptrace() with the above opcodes
        }

`HeapCallback` is registered with Breakpad to be called _before the dump is taken_. We use this hook to amend the `AppMemoryList`.
    
        bool HeapCallback(const void* crash_context, size_t crash_context_size, void* context) {
            VFPRCallback(crash_context, crash_context_size, context); // tune "ptrace"
            
            // << We have a different source of information for the crashing thread.>> -- Google
            // * google-breakpad/src/client/linux/minidump_writer/minidump_writer.cc:322
            ExceptionHandler::CrashContext* cc = (ExceptionHandler::CrashContext*) crash_context;
            uintptr_t stackTop = ToPage(UContextReader::GetStackPointer(&cc->context));
            
            // http://man7.org/linux/man-pages/man2/mincore.2.html
            if (IsPageMapped(stackTop)) {
                uintptr_t stackSize = page_size;
                
                // extra page. keep in sync with STACK_PAGES
                if (IsPageMapped(stackTop + page_size)) {
                    stackSize += page_size;
                }
    
                HeapContext* userContext = reinterpret_cast<HeapContext*>(context);
                userContext->CollectData(stackTop, stackSize);
            }
            
            return false; // we haven't handled - only prepared the handling
        }

`DumpCallback` can be used to generate multiple dumps. For instance, we can take two dumps of the same process, one regular and one "rich" ("caterpillar").

        bool DumpCallback(const MinidumpDescriptor& descriptor,
                          void* context,
                          bool succeeded) {
            return (--handler_count == 0) & succeeded;
        }

The following is the entry point of crash reporter registration. Its name should correspond to the respective "native" method in your Java wrapper, and arguments to the "native" method signature. It's possible to call it multiple times to create multiple (`handler_count`) dumps for a single crash.

        JNIEXPORT void JNICALL Java_com_example_breakpad_BreakpadWrapper_init(JNIEnv *env,
                jclass, jstring path, jboolean multiple, jboolean richDump) {
            if (handler_count > 0 && !multiple) {
                jclass exClass = env->FindClass("java/lang/IllegalStateException");
                env->ThrowNew( exClass,
                    "Registration of multiple handlers requires multiple flag set to true\0");
                return;
            }
            
            // http://man7.org/linux/man-pages/man3/sysconf.3.html
            page_size = sysconf(_SC_PAGESIZE);
            page_half = page_size >> 1;
            mem_count = page_size * STACK_PAGES / pt_size;
             
            dummyChunk.ptr = &dummyChunk;
            dummyChunk.length = 0;
    
            const char *convertedPath  = env->GetStringUTFChars(path, NULL);
            MinidumpDescriptor *descriptor = new MinidumpDescriptor(convertedPath);
    
            if (multiple && !richDump) {
                descriptor->set_size_limit(64 * 1024);
            }

            // signal handler registration is an ExceptionHandler constructor side effect;
            // upon exit, we are registered.

            if (richDump) {
                HeapContext* userContext = new HeapContext;
                ExceptionHandler* eh = new ExceptionHandler(*descriptor,
                        NULL, DumpCallback, userContext, true, -1);
                eh->set_crash_handler(HeapCallback); // includes VFPRCallback
                userContext->Attach(eh);
                EnsureBuf();
            } else {
                ExceptionHandler* eh = new ExceptionHandler(*descriptor, 
                        NULL, DumpCallback, NULL, true, -1);
                eh->set_crash_handler(VFPRCallback);
            }
            
            ++ handler_count;
        }
    }

...and don't forget
    
    jint JNI_OnLoad(JavaVM* vm, void* reserved) {
        return JNI_VERSION_1_6;
    }
    
That's it! Now your dumps contain the "sweet cuts" of the application heap.