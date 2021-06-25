namespace LED.Managers
{
    #pragma warning disable SA1401, SA1512
    using System;
    using System.Collections.Generic;
    using System.ComponentModel;
    using System.Diagnostics;
    using System.IO;
    using System.Net.Mail;
    using System.Threading;
    using System.Threading.Tasks;
    using LED.Common;
    using LED.Dao;
    using LED.Dao.Model;
    using LED.Dao.Repository;

    public class ManagerEmail : IServiceSender
    {
        // CONFIGURACIÓN PARA SERVIDOR SMTP
        public static SmtpConfiguration _smtpConf;

        private static readonly ManagerEmail _INSTANCE = new ManagerEmail();
        private static readonly int NextEmailsBatchWaiting = 60; // segundos para el próximo envío (por lotes)
        private static readonly string EmailMessageHeaderTemplateFilePath = @"C:\Users\lsarmiento.externos\Desktop\EmailMessageHeaderTemplate.html";
        private static readonly string EmailMessageFooterTemplateFilePath = @"C:\Users\lsarmiento.externos\Desktop\EmailMessageFooterTemplate.html";
        private static string _emailMessageHeaderTemplate = string.Empty;
        private static string _emailMessageFooterTemplate = string.Empty;

        private static bool _isRunning = false;
        private static Thread _emailSenderService; // thread principal

        //private EmailRecord email;// guarda el objeto email a enviar en cada thread
        //private bool mailSent = false;// indica si el intento de envío culminó

        private ManagerEmail()
        {
        }

        public static ManagerEmail GetInstance()
        {
            return _INSTANCE;
        }

        public static async Task<bool> EnqueueEmailForSending(EmailRecord email)
        {
            if (email.Recipients == null || email.Recipients == string.Empty)
            {
                return false;
            }

            if (email.PriorityForRecipient < 1 || email.PriorityForRecipient > 3)
            {
                email.PriorityForRecipient = 2;
            }

            if (email.PriorityForSending < 1 || email.PriorityForSending > 3)
            {
                email.PriorityForSending = 2;
            }

            email.Status = 0;
            email.RequestTimestamp = (ulong)DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1)).TotalSeconds;

            using (var context = DbContextFactory.GetContext())
            {
                EmailRecordRepository err = new EmailRecordRepository(context);
                await err.Create(email);
            }

            return true;
        }

        public void Initialize()
        {
            if (!_isRunning)
            {
                if (_smtpConf == null)
                {
                    Console.WriteLine("Debe setearse la variable static _smtpConf previamente!");
                    throw new Exception();
                }

                _isRunning = true;

                _emailSenderService = new Thread(this.StartEmailSenderService);
                _emailSenderService.Start();

                Console.WriteLine("Email service started!");
            }
        }

        public void Stop()
        {
            _isRunning = false;
            try
            {
                _emailSenderService.Abort();
            }
            catch (Exception)
            {
            }

            Console.WriteLine("Email service stoped! > please wait until it ends..");
        }

        private void StartEmailSenderService()
        {
            System.Net.ServicePointManager.DefaultConnectionLimit = 5000;
            System.Net.ServicePointManager.UseNagleAlgorithm = false;

            do
            {
                // RETRIEVE HEADER AND FOOTER TEMPLATES
                this.RetrieveHeaderAndFooterTemplates();

                EmailsBatchProcessor emailProcessor = new EmailsBatchProcessor(
                    _smtpConf,
                    _emailMessageHeaderTemplate,
                    _emailMessageFooterTemplate);

                Thread senderThread = new Thread(emailProcessor.SendEmailsBatch);
                senderThread.Start();

                // en vez de utilizar un sleep que bloquee hasta terminar, se utiliza este ciclo el cual culmina si cambia la variable isRunning
                // finalmente no culmina hasta no terminar de enviar los correos encolados!
                int initialUnixTS = (int)DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1)).TotalSeconds;
                int diff = 0;
                do
                {
                    diff = ((int)DateTime.UtcNow.Subtract(new DateTime(1970, 1, 1)).TotalSeconds) - initialUnixTS;
                    Thread.Sleep(1000);
                }
                while (diff < NextEmailsBatchWaiting && _isRunning);

            }
            while (_isRunning);
        }

        private void RetrieveHeaderAndFooterTemplates()
        {
            try
            {
                _emailMessageHeaderTemplate = File.ReadAllText(EmailMessageHeaderTemplateFilePath);
            }
            catch (Exception)
            {
                Console.WriteLine($"Error retrieving text from {EmailMessageHeaderTemplateFilePath}");
            }

            try
            {
                _emailMessageFooterTemplate = File.ReadAllText(EmailMessageFooterTemplateFilePath);
            }
            catch (Exception)
            {
                Console.WriteLine($"Error retrieving text from {EmailMessageFooterTemplateFilePath}");
            }
        }

    }
}